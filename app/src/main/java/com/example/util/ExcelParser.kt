package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ParsedSpreadsheet(
  val fileName: String,
  val headers: List<String>,
  val rows: List<Map<String, String>>,
  val totalRowCount: Int
)

object ExcelParser {
  private const val TAG = "ExcelParser"

  /**
   * Parse either .xlsx or .csv from an input stream
   */
  fun parse(inputStream: InputStream, fileName: String): ParsedSpreadsheet {
    return if (fileName.endsWith(".xlsx", ignoreCase = true)) {
      parseXlsx(inputStream, fileName)
    } else {
      parseCsv(inputStream, fileName)
    }
  }

  fun parseUri(context: Context, uri: Uri, fileName: String): ParsedSpreadsheet {
    val inputStream = context.contentResolver.openInputStream(uri)
      ?: throw IllegalArgumentException("Cannot open stream for uri: $uri")
    return inputStream.use { parse(it, fileName) }
  }

  /**
   * Fast, pure Kotlin .xlsx parser using Android built-in ZipInputStream and XmlPullParser.
   * No heavy external libraries required.
   */
  fun parseXlsx(inputStream: InputStream, fileName: String): ParsedSpreadsheet {
    var sharedStrings = listOf<String>()
    var sheetDataRows = mutableListOf<List<String>>()

    // First, pass through zip entries to extract sharedStrings and sheet1
    // Because zip streams are forward-only, we can buffer entries in memory or do a two-pass if needed.
    // Reading into a map of bytes for relevant files is very fast for exports (<10MB).
    val zipEntries = mutableMapOf<String, ByteArray>()
    ZipInputStream(inputStream).use { zis ->
      var entry: ZipEntry? = zis.nextEntry
      while (entry != null) {
        val name = entry.name
        if (name.equals("xl/sharedStrings.xml", ignoreCase = true) ||
          name.contains("worksheets/sheet1.xml", ignoreCase = true)
        ) {
          zipEntries[name] = zis.readBytes()
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    // 1. Parse xl/sharedStrings.xml if present
    val sharedStringsBytes = zipEntries.entries.find { it.key.contains("sharedStrings.xml", ignoreCase = true) }?.value
    if (sharedStringsBytes != null) {
      sharedStrings = parseSharedStrings(sharedStringsBytes)
    }

    // 2. Parse xl/worksheets/sheet1.xml
    val sheetBytes = zipEntries.entries.find { it.key.contains("sheet1.xml", ignoreCase = true) }?.value
    if (sheetBytes != null) {
      sheetDataRows = parseSheetData(sheetBytes, sharedStrings)
    }

    if (sheetDataRows.isEmpty()) {
      return ParsedSpreadsheet(fileName, emptyList(), emptyList(), 0)
    }

    // Header row is first row
    val rawHeaders = sheetDataRows.first().map { it.trim() }
    val cleanHeaders = rawHeaders.mapIndexed { index, h ->
      if (h.isNotBlank()) h else "Column_${index + 1}"
    }

    val dataRows = mutableListOf<Map<String, String>>()
    for (i in 1 until sheetDataRows.size) {
      val rowValues = sheetDataRows[i]
      if (rowValues.all { it.isBlank() }) continue // skip empty rows

      val rowMap = mutableMapOf<String, String>()
      cleanHeaders.forEachIndexed { colIdx, header ->
        val cellVal = if (colIdx < rowValues.size) rowValues[colIdx].trim() else ""
        rowMap[header] = cellVal
      }
      dataRows.add(rowMap)
    }

    return ParsedSpreadsheet(
      fileName = fileName,
      headers = cleanHeaders,
      rows = dataRows,
      totalRowCount = dataRows.size
    )
  }

  private fun parseSharedStrings(bytes: ByteArray): List<String> {
    val list = mutableListOf<String>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(bytes.inputStream(), "UTF-8")

    var eventType = parser.eventType
    val currentString = StringBuilder()
    var insideSi = false
    var insideT = false

    while (eventType != XmlPullParser.END_DOCUMENT) {
      when (eventType) {
        XmlPullParser.START_TAG -> {
          when (parser.name) {
            "si" -> {
              insideSi = true
              currentString.setLength(0)
            }
            "t" -> insideT = true
          }
        }
        XmlPullParser.TEXT -> {
          if (insideSi && insideT) {
            currentString.append(parser.text)
          }
        }
        XmlPullParser.END_TAG -> {
          when (parser.name) {
            "t" -> insideT = false
            "si" -> {
              insideSi = false
              list.add(currentString.toString())
            }
          }
        }
      }
      eventType = parser.next()
    }
    return list
  }

  private fun parseSheetData(bytes: ByteArray, sharedStrings: List<String>): MutableList<List<String>> {
    val allRows = mutableListOf<List<String>>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(bytes.inputStream(), "UTF-8")

    var eventType = parser.eventType
    var currentRow = mutableListOf<String>()
    var currentCellType = ""
    var currentCellValue = StringBuilder()
    var insideV = false
    var insideT = false
    var insideRow = false
    var currentColIndex = 0

    while (eventType != XmlPullParser.END_DOCUMENT) {
      when (eventType) {
        XmlPullParser.START_TAG -> {
          when (parser.name) {
            "row" -> {
              insideRow = true
              currentRow = mutableListOf()
              currentColIndex = 0
            }
            "c" -> {
              currentCellType = parser.getAttributeValue(null, "t") ?: ""
              val cellRef = parser.getAttributeValue(null, "r")
              if (cellRef != null) {
                val targetCol = getColumnIndexFromRef(cellRef)
                while (currentRow.size < targetCol) {
                  currentRow.add("")
                }
              }
              currentCellValue.setLength(0)
            }
            "v" -> insideV = true
            "t" -> insideT = true
          }
        }
        XmlPullParser.TEXT -> {
          if (insideV || insideT) {
            currentCellValue.append(parser.text)
          }
        }
        XmlPullParser.END_TAG -> {
          when (parser.name) {
            "v" -> insideV = false
            "t" -> insideT = false
            "c" -> {
              val raw = currentCellValue.toString().trim()
              val resolvedVal = when (currentCellType) {
                "s" -> {
                  val sIndex = raw.toIntOrNull()
                  if (sIndex != null && sIndex in sharedStrings.indices) {
                    sharedStrings[sIndex]
                  } else {
                    raw
                  }
                }
                "b" -> if (raw == "1") "TRUE" else "FALSE"
                else -> raw
              }
              currentRow.add(resolvedVal)
            }
            "row" -> {
              insideRow = false
              allRows.add(currentRow)
            }
          }
        }
      }
      eventType = parser.next()
    }
    return allRows
  }

  private fun getColumnIndexFromRef(cellRef: String): Int {
    var col = 0
    for (char in cellRef) {
      if (char in 'A'..'Z') {
        col = col * 26 + (char - 'A' + 1)
      } else {
        break
      }
    }
    return maxOf(0, col - 1)
  }

  /**
   * Fast CSV Parser supporting quotes and comma separation
   */
  fun parseCsv(inputStream: InputStream, fileName: String): ParsedSpreadsheet {
    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
    val rows = mutableListOf<List<String>>()

    var line = reader.readLine()
    // Handle UTF-8 BOM if present
    if (line != null && line.startsWith("\uFEFF")) {
      line = line.substring(1)
    }

    while (line != null) {
      val parsedLine = parseCsvLine(line)
      if (parsedLine.isNotEmpty() && !parsedLine.all { it.isBlank() }) {
        rows.add(parsedLine)
      }
      line = reader.readLine()
    }

    if (rows.isEmpty()) {
      return ParsedSpreadsheet(fileName, emptyList(), emptyList(), 0)
    }

    val cleanHeaders = rows.first().mapIndexed { i, h ->
      val trimmed = h.trim()
      if (trimmed.isNotBlank()) trimmed else "Column_${i + 1}"
    }

    val dataRows = mutableListOf<Map<String, String>>()
    for (i in 1 until rows.size) {
      val rowValues = rows[i]
      val rowMap = mutableMapOf<String, String>()
      cleanHeaders.forEachIndexed { index, header ->
        val value = if (index < rowValues.size) rowValues[index].trim() else ""
        rowMap[header] = value
      }
      dataRows.add(rowMap)
    }

    return ParsedSpreadsheet(
      fileName = fileName,
      headers = cleanHeaders,
      rows = dataRows,
      totalRowCount = dataRows.size
    )
  }

  private fun parseCsvLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false

    var i = 0
    while (i < line.length) {
      val c = line[i]
      when {
        c == '\"' -> {
          if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
            current.append('\"')
            i++ // skip escaped quote
          } else {
            inQuotes = !inQuotes
          }
        }
        c == ',' && !inQuotes -> {
          tokens.add(current.toString())
          current.setLength(0)
        }
        else -> current.append(c)
      }
      i++
    }
    tokens.add(current.toString())
    return tokens
  }
}
