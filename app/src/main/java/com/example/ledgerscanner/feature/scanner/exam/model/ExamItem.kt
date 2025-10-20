package com.example.ledgerscanner.feature.scanner.exam.model
enum class ExamStatus {
    DRAFT,      // template created, not scanned yet
    ACTIVE,     // currently scanning / running
    COMPLETED,  // all sheets scanned, stats available
    ARCHIVED    // no longer used
}