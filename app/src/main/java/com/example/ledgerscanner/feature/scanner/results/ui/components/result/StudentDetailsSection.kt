package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult

@Composable
fun StudentDetailsSection(
    barcodeId: String?,
    enrollmentNumber: String? = null,
    studentDetailsRef: MutableState<StudentDetailsForScanResult>,
    barcodeLocked: Boolean,
    onBarcodeChange: (String) -> Unit,
    onScanBarcode: () -> Unit
) {
    var studentName by remember { mutableStateOf<String?>(null) }
    var rollNumber by remember { mutableStateOf<Int?>(null) }
    val barcodeValue = barcodeId

    LaunchedEffect(studentName, rollNumber, barcodeId) {
        studentDetailsRef.value = StudentDetailsForScanResult(
            studentName,
            rollNumber,
            barcodeValue
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Student Details",
                style = AppTypography.label2Bold,
                color = Grey900
            )

            // Show enrollment number if detected from OMR sheet
            if (!enrollmentNumber.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))

                GenericTextField(
                    value = enrollmentNumber,
                    label = "Enrollment Number (auto-detected)",
                    onValueChange = { }, // read-only
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            GenericTextField(
                value = studentName ?: "",
                label = "Name",
                onValueChange = { studentName = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            GenericTextField(
                value = rollNumber?.toString() ?: "",
                label = "Roll number",
                onValueChange = { rollNumber = it.toIntOrNull() },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            if (barcodeId.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GenericTextField(
                        value = barcodeValue ?: "",
                        label = "Barcode Id",
                        onValueChange = { value ->
                            if (!barcodeLocked) {
                                onBarcodeChange(value)
                            }
                        },
                        readOnly = barcodeLocked,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onScanBarcode) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = Blue500
                        )
                    }
                }
            } else {
                GenericTextField(
                    value = barcodeValue ?: "",
                    label = "Barcode Id",
                    onValueChange = { value ->
                        if (!barcodeLocked) {
                            onBarcodeChange(value)
                        }
                    },
                    readOnly = barcodeLocked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
