package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object PatientList : Screen("patient_list", "Patients")
    object PatientDetail : Screen("patient_detail/{patientId}", "Patient Profile") {
        fun createRoute(patientId: Long) = "patient_detail/$patientId"
    }
    object NewDiagnostic : Screen("new_diagnostic/{patientId}", "New Diagnostic") {
        fun createRoute(patientId: Long) = "new_diagnostic/$patientId"
    }
    object PredictionDetail : Screen("prediction_detail/{predictionId}", "AI Diagnostic Report") {
        fun createRoute(predictionId: Long) = "prediction_detail/$predictionId"
    }
    object FederatedPrivacy : Screen("federated_privacy", "Federated Privacy")
    object Analytics : Screen("analytics", "Analytics")
    object ModelEvaluation : Screen("model_evaluation", "Model Evaluation")
}
