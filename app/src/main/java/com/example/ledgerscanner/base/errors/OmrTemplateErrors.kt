package com.example.ledgerscanner.base.errors

object OmrTemplateErrors {
    const val TEMPLATE_NOT_FOUND = "Template file not found in assets."
    const val TEMPLATE_DECODE_FAILED = "Failed to decode the template JSON."
    const val TEMPLATE_INVALID_FORMAT = "Template file is not in the expected format."
    const val TEMPLATE_ANCHORS_MISSING = "Template JSON missing anchor points."
    const val TEMPLATE_EMPTY_QUESTIONS = "Template has no questions defined."
    const val TEMPLATE_UNSUPPORTED_VERSION = "Template version not supported."
    const val TEMPLATE_LOAD_UNKNOWN_ERROR = "Unknown error occurred while loading template."
}