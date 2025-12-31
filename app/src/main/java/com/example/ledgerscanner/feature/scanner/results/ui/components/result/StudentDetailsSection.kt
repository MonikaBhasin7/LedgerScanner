package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult

@Composable
fun StudentDetailsSection(
    barcodeId: String?,
    studentDetailsRef: MutableState<StudentDetailsForScanResult>
) {
    var studentName by remember { mutableStateOf<String?>(null) }
    var rollNumber by remember { mutableStateOf<Int?>(null) }
    var barcodeValue by remember { mutableStateOf(barcodeId) }

    LaunchedEffect(studentName, rollNumber, barcodeValue) {
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

            GenericTextField(
                value = barcodeValue ?: "",
                label = "Barcode Id",
                onValueChange = { barcodeValue = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}