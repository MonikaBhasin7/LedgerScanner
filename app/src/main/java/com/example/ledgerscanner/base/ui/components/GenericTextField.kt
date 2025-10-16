package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.utils.ui.genericClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    isError: Boolean = false,
    isPassword: Boolean = false,
    textStyle: TextStyle = AppTypography.body2Medium,
    labelStyle: TextStyle = AppTypography.label2Medium,
    placeholderStyle: TextStyle = AppTypography.body3Regular,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: (() -> Unit)? = null,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Grey200,
        errorContainerColor = Color.Transparent,
        focusedIndicatorColor = Blue500,
        unfocusedIndicatorColor = Grey200,
        disabledIndicatorColor = Grey200,
        errorIndicatorColor = MaterialTheme.colorScheme.error,
        cursorColor = Blue500,
        focusedLabelColor = Blue500,
        unfocusedLabelColor = Grey500
    )
) {
    val effectiveColors: TextFieldColors = if (!enabled && onClick != null) {
        TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Blue500,
            unfocusedIndicatorColor = Grey200,
            disabledIndicatorColor = Grey200,
            errorIndicatorColor = MaterialTheme.colorScheme.error,
            cursorColor = Blue500,
            focusedLabelColor = Blue500,
            unfocusedLabelColor = Grey500,
            disabledTextColor = Black
        )
    } else {
        colors
    }


    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.then(
            if (onClick != null) Modifier.genericClick { onClick() } else Modifier
        )) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            textStyle = textStyle,
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            shape = shape,
            interactionSource = interactionSource,
            colors = effectiveColors,

            // Label
            label = label?.let { { Text(it, style = labelStyle) } },

            // Placeholder
            placeholder = placeholder?.let {
                { Text(it, style = placeholderStyle, color = Grey500) }
            },

            // Icons
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon ?: if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible)
                                "Hide password"
                            else
                                "Show password"
                        )
                    }
                }
            } else null,

            // Additional slots
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,

            // Visual transformation for password
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,

            // Keyboard
            keyboardOptions = if (isPassword)
                keyboardOptions.copy(keyboardType = KeyboardType.Password)
            else
                keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}