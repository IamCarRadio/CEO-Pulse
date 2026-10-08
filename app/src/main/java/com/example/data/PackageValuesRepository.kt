package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.PackageValueSetting
import org.json.JSONArray
import org.json.JSONObject

class PackageValuesRepository(private val context: Context) {
  private val prefs = context.getSharedPreferences("ceo_pulse_package_values", Context.MODE_PRIVATE)

  // Default baseline package valuation rules
  private val defaultPackageRules = listOf(
    PackageValueSetting(
      serviceName = "Full-Face Laser Genesis Resurfacing",
      category = "Skin & Dermatology",
      perSessionValue = 350.00,
      notes = "Prepaid 5-Session Laser Package"
    ),
    PackageValueSetting(
      serviceName = "Medical Grade Chemical Peel (TCA)",
      category = "Skin & Dermatology",
      perSessionValue = 220.00,
      notes = "Enrolled Acne & Pigmentation Package"
    ),
    PackageValueSetting(
      serviceName = "Dermatological Cryotherapy & Lesion Removal",
      category = "Skin & Dermatology",
      perSessionValue = 160.00,
      notes = "Annual Skin Health Enrollment"
    ),
    PackageValueSetting(
      serviceName = "GLP-1 Medical Weight Protocol Ingestion",
      category = "Obesity & Weight Management",
      perSessionValue = 550.00,
      notes = "12-Week Metabolic Transformation"
    ),
    PackageValueSetting(
      serviceName = "Platelet-Rich Plasma (PRP) Scalp Therapy",
      category = "Hair Restoration",
      perSessionValue = 450.00,
      notes = "6-Session Hair Restoration Regimen"
    ),
    PackageValueSetting(
      serviceName = "Dental Prophylaxis & Ultrasonic Scaling",
      category = "Dental Medicine",
      perSessionValue = 180.00,
      notes = "Family Preventive Care Package"
    ),
    PackageValueSetting(
      serviceName = "Executive Comprehensive Health Exam",
      category = "General Consultation",
      perSessionValue = 300.00,
      notes = "Corporate Executive Annual Plan"
    )
  )

  init {
    if (!prefs.contains("package_rules_initialized")) {
      defaultPackageRules.forEach { setting ->
        savePackageValue(setting.serviceName, setting.perSessionValue, setting.category, setting.notes)
      }
      prefs.edit().putBoolean("package_rules_initialized", true).apply()
    }
  }

  fun getPackageValuesMap(): Map<String, Double> {
    val list = getPackageValueSettingsList()
    return list.associate { it.serviceName.trim() to it.perSessionValue }
  }

  fun getPackageValueSettingsList(): List<PackageValueSetting> {
    val jsonString = prefs.getString("package_rules_list", null) ?: return defaultPackageRules
    return try {
      val jsonArray = JSONArray(jsonString)
      val list = mutableListOf<PackageValueSetting>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          PackageValueSetting(
            serviceName = obj.getString("serviceName"),
            category = obj.optString("category", "Clinical"),
            perSessionValue = obj.getDouble("perSessionValue"),
            notes = obj.optString("notes", "")
          )
        )
      }
      list.sortedBy { it.serviceName }
    } catch (e: Exception) {
      Log.e("PackageValuesRepo", "Error reading package rules", e)
      defaultPackageRules
    }
  }

  fun savePackageValue(serviceName: String, perSessionValue: Double, category: String = "Clinical", notes: String = "") {
    val list = getPackageValueSettingsList().toMutableList()
    list.removeAll { it.serviceName.trim().equals(serviceName.trim(), ignoreCase = true) }
    list.add(
      PackageValueSetting(
        serviceName = serviceName.trim(),
        category = category.trim(),
        perSessionValue = perSessionValue,
        notes = notes
      )
    )

    val jsonArray = JSONArray()
    list.forEach { setting ->
      val obj = JSONObject().apply {
        put("serviceName", setting.serviceName)
        put("category", setting.category)
        put("perSessionValue", setting.perSessionValue)
        put("notes", setting.notes)
      }
      jsonArray.put(obj)
    }

    prefs.edit().putString("package_rules_list", jsonArray.toString()).apply()
  }

  fun deletePackageValue(serviceName: String) {
    val list = getPackageValueSettingsList().toMutableList()
    list.removeAll { it.serviceName.trim().equals(serviceName.trim(), ignoreCase = true) }

    val jsonArray = JSONArray()
    list.forEach { setting ->
      val obj = JSONObject().apply {
        put("serviceName", setting.serviceName)
        put("category", setting.category)
        put("perSessionValue", setting.perSessionValue)
        put("notes", setting.notes)
      }
      jsonArray.put(obj)
    }

    prefs.edit().putString("package_rules_list", jsonArray.toString()).apply()
  }
}
