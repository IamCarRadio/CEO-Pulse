package com.example.util

object SampleExportData {

  val HEADERS = listOf(
    "InvoiceNo",
    "Date",
    "PatientId",
    "Doctor",
    "Branch",
    "Category",
    "ServiceName",
    "Quantity",
    "UnitPrice",
    "Discount",
    "NetTotal",
    "PaymentMethod"
  )

  fun getServiceSalesSampleRows(date: String = "2026-10-04", branch: String = "Downtown Executive Clinic"): List<Map<String, String>> {
    return listOf(
      mapOf(
        "InvoiceNo" to "INV-2026-8091",
        "Date" to date,
        "PatientId" to "PT-10492",
        "Doctor" to "Dr. Sarah Jenkins, MD",
        "Branch" to branch,
        "Category" to "General Consultation",
        "ServiceName" to "Executive Comprehensive Health Assessment",
        "Quantity" to "1",
        "UnitPrice" to "350.00",
        "Discount" to "0.00",
        "NetTotal" to "350.00",
        "PaymentMethod" to "Corporate Insurance"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8092",
        "Date" to date,
        "PatientId" to "PT-10493",
        "Doctor" to "Dr. Michael Chen, MD",
        "Branch" to branch,
        "Category" to "Diagnostics & Lab",
        "ServiceName" to "12-Lead Electrocardiogram (ECG) with Report",
        "Quantity" to "1",
        "UnitPrice" to "185.00",
        "Discount" to "15.00",
        "NetTotal" to "170.00",
        "PaymentMethod" to "Credit Card"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8093",
        "Date" to date,
        "PatientId" to "PT-10494",
        "Doctor" to "Dr. Lisa Ray, DDS",
        "Branch" to branch,
        "Category" to "Dental Medicine",
        "ServiceName" to "Ultrasonic Scaling & Dental Prophylaxis",
        "Quantity" to "1",
        "UnitPrice" to "220.00",
        "Discount" to "0.00",
        "NetTotal" to "220.00",
        "PaymentMethod" to "Direct Billing"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8094",
        "Date" to date,
        "PatientId" to "PT-10495",
        "Doctor" to "Dr. Emily Rodriguez, MD",
        "Branch" to branch,
        "Category" to "Specialist Care",
        "ServiceName" to "Pediatric Developmental Wellness Screening",
        "Quantity" to "1",
        "UnitPrice" to "195.00",
        "Discount" to "0.00",
        "NetTotal" to "195.00",
        "PaymentMethod" to "Cash"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8095",
        "Date" to date,
        "PatientId" to "PT-10496",
        "Doctor" to "Dr. David Alabi, MD",
        "Branch" to branch,
        "Category" to "Specialist Care",
        "ServiceName" to "Orthopedic Joint Assessment & Ultrasound",
        "Quantity" to "1",
        "UnitPrice" to "290.00",
        "Discount" to "20.00",
        "NetTotal" to "270.00",
        "PaymentMethod" to "Insurance"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8096",
        "Date" to date,
        "PatientId" to "PT-10497",
        "Doctor" to "Dr. Sarah Jenkins, MD",
        "Branch" to branch,
        "Category" to "Diagnostics & Lab",
        "ServiceName" to "Complete Metabolic Panel & Lipid Panel",
        "Quantity" to "1",
        "UnitPrice" to "145.00",
        "Discount" to "0.00",
        "NetTotal" to "145.00",
        "PaymentMethod" to "Credit Card"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8097",
        "Date" to date,
        "PatientId" to "PT-10498",
        "Doctor" to "Dr. Lisa Ray, DDS",
        "Branch" to branch,
        "Category" to "Dental Medicine",
        "ServiceName" to "Full Mouth Digital Panoramic X-Ray",
        "Quantity" to "1",
        "UnitPrice" to "110.00",
        "Discount" to "10.00",
        "NetTotal" to "100.00",
        "PaymentMethod" to "Direct Billing"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8098",
        "Date" to date,
        "PatientId" to "PT-10499",
        "Doctor" to "Dr. Michael Chen, MD",
        "Branch" to branch,
        "Category" to "General Consultation",
        "ServiceName" to "Hypertension Management Follow-up",
        "Quantity" to "1",
        "UnitPrice" to "130.00",
        "Discount" to "0.00",
        "NetTotal" to "130.00",
        "PaymentMethod" to "Credit Card"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8099",
        "Date" to date,
        "PatientId" to "PT-10500",
        "Doctor" to "Dr. David Alabi, MD",
        "Branch" to branch,
        "Category" to "Surgical Procedures",
        "ServiceName" to "Minor Tendon Infiltration & Local Steroid",
        "Quantity" to "1",
        "UnitPrice" to "260.00",
        "Discount" to "0.00",
        "NetTotal" to "260.00",
        "PaymentMethod" to "Insurance"
      ),
      mapOf(
        "InvoiceNo" to "INV-2026-8100",
        "Date" to date,
        "PatientId" to "PT-10501",
        "Doctor" to "Dr. Emily Rodriguez, MD",
        "Branch" to branch,
        "Category" to "Diagnostics & Lab",
        "ServiceName" to "Rapid Strep Antigen & PCR Swab",
        "Quantity" to "1",
        "UnitPrice" to "85.00",
        "Discount" to "0.00",
        "NetTotal" to "85.00",
        "PaymentMethod" to "Debit Card"
      )
    )
  }

  fun getCsvSampleString(date: String = "2026-10-04", branch: String = "Downtown Executive Clinic"): String {
    val sb = StringBuilder()
    sb.append(HEADERS.joinToString(",")).append("\n")
    getServiceSalesSampleRows(date, branch).forEach { row ->
      val line = HEADERS.map { header ->
        val cell = row[header] ?: ""
        if (cell.contains(",") || cell.contains("\"")) {
          "\"${cell.replace("\"", "\"\"")}\""
        } else {
          cell
        }
      }.joinToString(",")
      sb.append(line).append("\n")
    }
    return sb.toString()
  }

  val PHARMACY_HEADERS = listOf(
    "ProductCode",
    "ProductName",
    "Category",
    "Quantity",
    "UnitCost",
    "UnitPrice",
    "TotalCost",
    "TotalRevenue",
    "GrossMargin",
    "MarginPercentage"
  )

  fun getPharmacyMarginSampleRows(date: String = "2026-10-04", branch: String = "Downtown Executive Clinic"): List<Map<String, String>> {
    return listOf(
      mapOf(
        "ProductCode" to "RX-SEMA-24",
        "ProductName" to "Semaglutide 2.4mg/0.75ml Pen (Wegovy)",
        "Category" to "Metabolic & Weight",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "42",
        "UnitCost" to "260.00",
        "UnitPrice" to "380.00",
        "TotalCost" to "10920.00",
        "TotalRevenue" to "15960.00",
        "GrossMargin" to "5040.00",
        "MarginPercentage" to "31.58"
      ),
      mapOf(
        "ProductCode" to "RX-TIRZ-05",
        "ProductName" to "Tirzepatide 5mg Auto-Injector (Mounjaro)",
        "Category" to "Metabolic & Weight",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "30",
        "UnitCost" to "310.00",
        "UnitPrice" to "460.00",
        "TotalCost" to "9300.00",
        "TotalRevenue" to "13800.00",
        "GrossMargin" to "4500.00",
        "MarginPercentage" to "32.61"
      ),
      mapOf(
        "ProductCode" to "DERM-BTX-100",
        "ProductName" to "Botulinum Toxin Type A 100 Units (Botox Cosmetic)",
        "Category" to "Aesthetics & Injectables",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "28",
        "UnitCost" to "280.00",
        "UnitPrice" to "420.00",
        "TotalCost" to "7840.00",
        "TotalRevenue" to "11760.00",
        "GrossMargin" to "3920.00",
        "MarginPercentage" to "33.33"
      ),
      mapOf(
        "ProductCode" to "DERM-HA-JUV",
        "ProductName" to "Hyaluronic Acid Dermal Filler 1ml (Juvederm Ultra)",
        "Category" to "Aesthetics & Injectables",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "24",
        "UnitCost" to "220.00",
        "UnitPrice" to "360.00",
        "TotalCost" to "5280.00",
        "TotalRevenue" to "8640.00",
        "GrossMargin" to "3360.00",
        "MarginPercentage" to "38.89"
      ),
      mapOf(
        "ProductCode" to "DERM-TRET-05",
        "ProductName" to "Tretinoin 0.05% Microsphere Gel 45g",
        "Category" to "Dermatologicals",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "65",
        "UnitCost" to "38.00",
        "UnitPrice" to "85.00",
        "TotalCost" to "2470.00",
        "TotalRevenue" to "5525.00",
        "GrossMargin" to "3055.00",
        "MarginPercentage" to "55.29"
      ),
      mapOf(
        "ProductCode" to "HAIR-MNX-05",
        "ProductName" to "Minoxidil 5% Topical Scalp Solution 60ml",
        "Category" to "Hair Restoration",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "58",
        "UnitCost" to "22.00",
        "UnitPrice" to "65.00",
        "TotalCost" to "1276.00",
        "TotalRevenue" to "3770.00",
        "GrossMargin" to "2494.00",
        "MarginPercentage" to "66.15"
      ),
      mapOf(
        "ProductCode" to "HAIR-FIN-01",
        "ProductName" to "Finasteride 1mg Tablets 30s",
        "Category" to "Hair Restoration",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "44",
        "UnitCost" to "18.00",
        "UnitPrice" to "52.00",
        "TotalCost" to "792.00",
        "TotalRevenue" to "2288.00",
        "GrossMargin" to "1496.00",
        "MarginPercentage" to "65.39"
      ),
      mapOf(
        "ProductCode" to "CARD-ATV-20",
        "ProductName" to "Atorvastatin Calcium 20mg Tablets 90s",
        "Category" to "Cardiovascular",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "50",
        "UnitCost" to "25.00",
        "UnitPrice" to "55.00",
        "TotalCost" to "1250.00",
        "TotalRevenue" to "2750.00",
        "GrossMargin" to "1500.00",
        "MarginPercentage" to "54.55"
      ),
      mapOf(
        "ProductCode" to "ANTI-AMX-875",
        "ProductName" to "Amoxicillin / Clavulanic Acid 875/125mg 20s",
        "Category" to "Antibiotics",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "36",
        "UnitCost" to "32.00",
        "UnitPrice" to "68.00",
        "TotalCost" to "1152.00",
        "TotalRevenue" to "2448.00",
        "GrossMargin" to "1296.00",
        "MarginPercentage" to "52.94"
      ),
      mapOf(
        "ProductCode" to "MET-MET-500",
        "ProductName" to "Metformin Hydrochloride 500mg ER 100s",
        "Category" to "Chronic Care",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "60",
        "UnitCost" to "15.00",
        "UnitPrice" to "38.00",
        "TotalCost" to "900.00",
        "TotalRevenue" to "2280.00",
        "GrossMargin" to "1380.00",
        "MarginPercentage" to "60.53"
      ),
      mapOf(
        "ProductCode" to "DERM-HQ-04",
        "ProductName" to "Hydroquinone 4% Prescription Skin Cream 30g",
        "Category" to "Dermatologicals",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "32",
        "UnitCost" to "28.00",
        "UnitPrice" to "70.00",
        "TotalCost" to "896.00",
        "TotalRevenue" to "2240.00",
        "GrossMargin" to "1344.00",
        "MarginPercentage" to "60.00"
      ),
      mapOf(
        "ProductCode" to "SUPP-VTD-50K",
        "ProductName" to "Vitamin D3 50,000 IU High Potency 12s",
        "Category" to "Supplements",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "75",
        "UnitCost" to "8.00",
        "UnitPrice" to "28.00",
        "TotalCost" to "600.00",
        "TotalRevenue" to "2100.00",
        "GrossMargin" to "1500.00",
        "MarginPercentage" to "71.43"
      ),
      mapOf(
        "ProductCode" to "GI-OMP-20",
        "ProductName" to "Omeprazole Delayed-Release 20mg 30s",
        "Category" to "Gastroenterology",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "40",
        "UnitCost" to "12.00",
        "UnitPrice" to "30.00",
        "TotalCost" to "480.00",
        "TotalRevenue" to "1200.00",
        "GrossMargin" to "720.00",
        "MarginPercentage" to "60.00"
      ),
      mapOf(
        "ProductCode" to "OPH-CIP-03",
        "ProductName" to "Ciprofloxacin 0.3% Ophthalmic Drops 5ml",
        "Category" to "Ophthalmology",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "20",
        "UnitCost" to "16.00",
        "UnitPrice" to "35.00",
        "TotalCost" to "320.00",
        "TotalRevenue" to "700.00",
        "GrossMargin" to "380.00",
        "MarginPercentage" to "54.29"
      ),
      mapOf(
        "ProductCode" to "ALL-CET-10",
        "ProductName" to "Cetirizine Hydrochloride 10mg 30s",
        "Category" to "Allergy",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "25",
        "UnitCost" to "9.00",
        "UnitPrice" to "22.00",
        "TotalCost" to "225.00",
        "TotalRevenue" to "550.00",
        "GrossMargin" to "325.00",
        "MarginPercentage" to "59.09"
      ),
      mapOf(
        "ProductCode" to "DERM-MUP-02",
        "ProductName" to "Mupirocin 2% Topical Antibacterial Ointment 22g",
        "Category" to "Dermatologicals",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "18",
        "UnitCost" to "20.00",
        "UnitPrice" to "42.00",
        "TotalCost" to "360.00",
        "TotalRevenue" to "756.00",
        "GrossMargin" to "396.00",
        "MarginPercentage" to "52.38"
      ),
      mapOf(
        "ProductCode" to "DENT-CHX-01",
        "ProductName" to "Chlorhexidine Gluconate 0.12% Oral Rinse 473ml",
        "Category" to "Dental Dispensary",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "15",
        "UnitCost" to "14.00",
        "UnitPrice" to "28.00",
        "TotalCost" to "210.00",
        "TotalRevenue" to "420.00",
        "GrossMargin" to "210.00",
        "MarginPercentage" to "50.00"
      ),
      mapOf(
        "ProductCode" to "DERM-HC-01",
        "ProductName" to "Hydrocortisone 1% Anti-Inflammatory Cream 30g",
        "Category" to "Dermatologicals",
        "Date" to date,
        "Branch" to branch,
        "Quantity" to "12",
        "UnitCost" to "9.50",
        "UnitPrice" to "18.00",
        "TotalCost" to "114.00",
        "TotalRevenue" to "216.00",
        "GrossMargin" to "102.00",
        "MarginPercentage" to "47.22"
      )
    )
  }
}
