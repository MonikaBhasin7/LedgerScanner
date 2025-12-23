package com.example.ledgerscanner.base.errors

object ErrorMessages {

    // ============ General Errors ============
    const val UNKNOWN_ERROR = "An unknown error occurred"
    const val NETWORK_ERROR = "Network connection failed"
    const val OPERATION_FAILED = "Operation failed. Please try again"

    // ============ Validation Errors ============
    const val FIELD_REQUIRED = "This field is required"
    const val INVALID_INPUT = "Invalid input provided"
    const val INVALID_NUMBER = "Please enter a valid number"
    const val INVALID_RANGE = "Value must be within valid range"

    // ============ Exam Errors ============
    const val EXAM_NOT_FOUND = "Exam not found"
    const val EXAM_NAME_REQUIRED = "Exam name is required"
    const val EXAM_NAME_TOO_SHORT = "Exam name must be at least 3 characters"
    const val EXAM_DESCRIPTION_REQUIRED = "Description is required"
    const val EXAM_TEMPLATE_NOT_SELECTED = "Please select an answer sheet template"
    const val EXAM_QUESTIONS_INVALID = "Number of questions must be greater than 0"
    const val EXAM_QUESTIONS_EXCEEDS_TEMPLATE = "Number of questions exceeds template limit"
    const val EXAM_CREATE_FAILED = "Failed to create exam"
    const val EXAM_UPDATE_FAILED = "Failed to update exam"
    const val EXAM_DELETE_FAILED = "Failed to delete exam"
    const val EXAM_DUPLICATE_FAILED = "Failed to duplicate exam"
    const val EXAM_LOAD_FAILED = "Failed to load exam details"

    // ============ Answer Key Errors ============
    const val ANSWER_KEY_INCOMPLETE = "Please provide answers for all questions"
    const val ANSWER_KEY_SAVE_FAILED = "Failed to save answer key"
    const val ANSWER_KEY_NOT_SET = "Answer key not configured for this exam"
    const val ANSWER_KEY_INVALID_OPTION = "Invalid answer option selected"

    // ============ Marking Scheme Errors ============
    const val MARKS_PER_CORRECT_REQUIRED = "Marks per correct answer is required"
    const val MARKS_PER_CORRECT_INVALID = "Marks per correct must be greater than 0"
    const val MARKS_PER_WRONG_REQUIRED = "Marks per wrong answer is required"
    const val MARKS_PER_WRONG_INVALID = "Marks per wrong must be 0 or greater"
    const val MARKING_SCHEME_SAVE_FAILED = "Failed to save marking scheme"

    // ============ Template Errors ============
    const val TEMPLATE_NOT_FOUND = "Template file not found in assets"
    const val TEMPLATE_DECODE_FAILED = "Failed to decode the template JSON"
    const val TEMPLATE_INVALID_FORMAT = "Template file is not in the expected format"
    const val TEMPLATE_ANCHORS_MISSING = "Template JSON missing anchor points"
    const val TEMPLATE_EMPTY_QUESTIONS = "Template has no questions defined"
    const val TEMPLATE_UNSUPPORTED_VERSION = "Template version not supported"
    const val TEMPLATE_LOAD_UNKNOWN_ERROR = "Unknown error occurred while loading template"
    const val TEMPLATE_GENERATION_FAILED = "Failed to generate template from image"
    const val TEMPLATE_ANCHORS_NOT_DETECTED = "Could not detect 4 anchor points in image"
    const val TEMPLATE_BUBBLES_NOT_DETECTED = "Could not detect bubbles in image"
    const val TEMPLATE_BUBBLE_COUNT_MISMATCH = "Detected bubble count doesn't match expected count"

    // ============ Scanner/Camera Errors ============
    const val CAMERA_PERMISSION_DENIED = "Camera permission is required to scan sheets"
    const val CAMERA_PERMISSION_PERMANENTLY_DENIED =
        "Camera permission permanently denied. Please enable it in settings"
    const val CAMERA_INITIALIZATION_FAILED = "Failed to initialize camera"
    const val CAMERA_NOT_AVAILABLE = "Camera is not available on this device"
    const val IMAGE_CAPTURE_FAILED = "Failed to capture image"
    const val IMAGE_PROCESSING_FAILED = "Failed to process captured image"

    // ============ OMR Processing Errors ============
    const val ANCHORS_NOT_DETECTED =
        "Could not detect anchor points. Please align the sheet properly"
    const val ANCHORS_PARTIAL_DETECTION = "Only some anchors detected. Please include all 4 corners"
    const val ANCHORS_EXPECTED_COUNT_MISMATCH = "Expected 4 anchors, found different count"
    const val IMAGE_WARP_FAILED = "Failed to align image with template"
    const val BUBBLE_DETECTION_FAILED = "Failed to detect bubbles on sheet"
    const val BUBBLE_COUNT_MISMATCH = "Detected bubbles don't match template configuration"
    const val LOW_CONFIDENCE_DETECTION = "Some bubbles detected with low confidence"
    const val MULTIPLE_MARKS_DETECTED = "Multiple options marked for same question"
    const val EVALUATION_FAILED = "Failed to evaluate answers"

    // ============ Scan Result Errors ============
    const val SCAN_RESULT_SAVE_FAILED = "Failed to save scan result"
    const val SCAN_RESULT_NOT_FOUND = "Scan result not found"
    const val SCAN_RESULT_DELETE_FAILED = "Failed to delete scan result"
    const val SCAN_RESULTS_LOAD_FAILED = "Failed to load scan results"
    const val NO_SCANNED_SHEETS = "No sheets have been scanned yet"

    // ============ Database Errors ============
    const val DATABASE_ERROR = "Database operation failed"
    const val DATABASE_INSERT_FAILED = "Failed to insert record"
    const val DATABASE_UPDATE_FAILED = "Failed to update record"
    const val DATABASE_DELETE_FAILED = "Failed to delete record"
    const val DATABASE_QUERY_FAILED = "Failed to retrieve data"

    // ============ File/Storage Errors ============
    const val FILE_NOT_FOUND = "File not found"
    const val FILE_READ_FAILED = "Failed to read file"
    const val FILE_WRITE_FAILED = "Failed to write file"
    const val STORAGE_PERMISSION_DENIED = "Storage permission is required"
    const val INSUFFICIENT_STORAGE = "Insufficient storage space"
    const val IMAGE_LOAD_FAILED = "Failed to load image"
    const val IMAGE_SAVE_FAILED = "Failed to save image"

    // ============ Image Quality Errors ============
    const val IMAGE_TOO_DARK = "Image is too dark. Please ensure good lighting"
    const val IMAGE_TOO_BRIGHT = "Image is too bright. Reduce glare"
    const val IMAGE_GLARE_DETECTED = "Glare detected on sheet. Adjust lighting"
    const val IMAGE_BLURRY = "Image is too blurry. Hold steady while scanning"
    const val IMAGE_LOW_RESOLUTION = "Image resolution too low"

    // ============ Sheet Alignment Errors ============
    const val SHEET_NOT_IN_FRAME = "Sheet not in frame. Position it within the guide"
    const val SHEET_TOO_FAR = "Sheet is too far. Move closer"
    const val SHEET_TOO_CLOSE = "Sheet is too close. Move back slightly"
    const val SHEET_CORNERS_NOT_VISIBLE = "All 4 corners must be visible"
    const val SHEET_ANGLE_INCORRECT = "Adjust sheet angle for better alignment"

    // ============ Data Export Errors ============
    const val EXPORT_FAILED = "Failed to export data"
    const val EXPORT_PERMISSION_DENIED = "Permission required to export data"
    const val EXPORT_NO_DATA = "No data available to export"
    const val EXPORT_FORMAT_UNSUPPORTED = "Export format not supported"

    // ============ Search/Filter Errors ============
    const val SEARCH_FAILED = "Search operation failed"
    const val NO_RESULTS_FOUND = "No results found"
    const val FILTER_APPLY_FAILED = "Failed to apply filters"

    // ============ User Input Errors ============
    const val STUDENT_NAME_REQUIRED = "Student name is required"
    const val ROLL_NUMBER_REQUIRED = "Roll number is required"
    const val ROLL_NUMBER_INVALID = "Invalid roll number format"

    // ============ Network/Sync Errors ============
    const val SYNC_FAILED = "Failed to synchronize data"
    const val NO_INTERNET_CONNECTION = "No internet connection available"
    const val SERVER_ERROR = "Server error occurred"
    const val REQUEST_TIMEOUT = "Request timed out"

    // ============ OpenCV Errors ============
    const val OPENCV_NOT_INITIALIZED = "OpenCV library not initialized"
    const val OPENCV_OPERATION_FAILED = "OpenCV operation failed"
    const val MAT_CONVERSION_FAILED = "Failed to convert image format"

    // ============ Memory Errors ============
    const val OUT_OF_MEMORY = "Insufficient memory to complete operation"
    const val BITMAP_TOO_LARGE = "Image is too large to process"

    // ============ Guidance Messages (Not Errors) ============
    const val SCANNING_TIP_LIGHTING = "Ensure good lighting and avoid shadows"
    const val SCANNING_TIP_ALIGNMENT = "Fill the frame and include all corners"
    const val SCANNING_TIP_STEADINESS = "Hold steady for best results"

    // ============ Helper Functions ============

    /**
     * Format error with dynamic value
     */
    fun formatError(template: String, vararg args: Any): String {
        return String.format(template, *args)
    }

    /**
     * Get bubble count mismatch message
     */
    fun bubbleCountMismatchError(expected: Int, actual: Int): String {
        return "Bubble count mismatch: expected $expected, got $actual"
    }

    /**
     * Get anchor count mismatch message
     */
    fun anchorCountMismatchError(expected: Int, actual: Int): String {
        return "Expected $expected anchors, found $actual"
    }

    /**
     * Get questions exceeds template error
     */
    fun questionsExceedsTemplateError(requested: Int, maximum: Int): String {
        return "Requested $requested questions exceeds template maximum of $maximum"
    }

    /**
     * Get low confidence questions message
     */
    fun lowConfidenceQuestionsWarning(count: Int): String {
        return "$count question${if (count > 1) "s" else ""} detected with low confidence"
    }

    /**
     * Get multiple marks warning
     */
    fun multipleMarksWarning(questionNumbers: List<Int>): String {
        return "Multiple answers marked for questions: ${questionNumbers.joinToString(", ")}"
    }
}