package com.example.model

enum class DashboardTab(val title: String, val subtitle: String) {
  DASHBOARD("Dashboard", "Executive Group Overview & Clinic Performance"),
  SERVICES("Services", "Clinical Specialties, Procedures & Service Lines"),
  PHARMACY("Pharmacy", "Dispensary Operations, Formulary & Rx Margins"),
  UPLOAD("Upload", "Secure Billing & EHR Data Feed Integration"),
  AI("Ask", "Executive AI Chat Agent"),
  USERS("Users & Access", "User Accounts & Role Permissions")
}
