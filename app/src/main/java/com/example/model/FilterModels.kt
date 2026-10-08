package com.example.model

enum class DatePreset(val label: String) {
  TODAY("Today"),
  YESTERDAY("Yesterday"),
  SEVEN_DAYS("7D"),
  THIRTY_DAYS("30D"),
  THIS_MONTH("This Month"),
  CUSTOM("Custom")
}

data class FilterState(
  val datePreset: DatePreset = DatePreset.THIS_MONTH,
  val customStartDate: String = "2026-10-01",
  val customEndDate: String = "2026-10-03",
  val selectedBranch: String = "All Branches",
  val selectedDoctor: String = "All Doctors",
  val selectedCategory: String = "All Categories",
  val isCustomDatePickerOpen: Boolean = false
) {
  companion object {
    val AVAILABLE_BRANCHES = listOf(
      "All Branches",
      "Downtown Executive Clinic",
      "Northside Medical Plaza",
      "West End Health Center",
      "Metro Central Clinic",
      "Harbor Point Care"
    )

    val AVAILABLE_DOCTORS = listOf(
      "All Doctors",
      "Dr. Sarah Jenkins, MD (Chief Medical)",
      "Dr. Michael Chen, MD (Cardiology)",
      "Dr. Emily Rodriguez, MD (Pediatrics)",
      "Dr. David Alabi, MD (Orthopedics)",
      "Dr. Lisa Ray, DDS (Dental Director)"
    )

    val AVAILABLE_CATEGORIES = listOf(
      "All Categories",
      "General Consultation",
      "Specialist Care",
      "Dental Medicine",
      "Pharmacy & Rx",
      "Lab Diagnostics",
      "Surgical Procedures"
    )
  }

  val activeFilterCount: Int
    get() {
      var count = 0
      if (datePreset != DatePreset.THIS_MONTH) count++
      if (selectedBranch != "All Branches") count++
      if (selectedDoctor != "All Doctors") count++
      if (selectedCategory != "All Categories") count++
      return count
    }

  val formattedDateDisplay: String
    get() = when (datePreset) {
      DatePreset.TODAY -> "Today (Oct 3, 2026)"
      DatePreset.YESTERDAY -> "Yesterday (Oct 2, 2026)"
      DatePreset.SEVEN_DAYS -> "Past 7 Days (Sep 27 - Oct 3)"
      DatePreset.THIRTY_DAYS -> "Past 30 Days (Sep 3 - Oct 3)"
      DatePreset.THIS_MONTH -> "This Month (October 2026)"
      DatePreset.CUSTOM -> "$customStartDate to $customEndDate"
    }
}
