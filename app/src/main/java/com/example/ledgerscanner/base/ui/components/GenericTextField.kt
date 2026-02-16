package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
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
    textStyle: TextStyle = AppTypography.text15Medium,
    labelStyle: TextStyle = AppTypography.text14Medium,
    placeholderStyle: TextStyle = AppTypography.text14Regular,
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
        focusedIndicatorColor = Blue500.copy(alpha = 0.72f),
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
    val fieldModifier = modifier
        .then(if (onClick != null) Modifier.genericClick { onClick() } else Modifier)

    val visualTransformation = if (isPassword && !passwordVisible) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = fieldModifier,
        textStyle = textStyle.copy(color = Black),
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(Blue500),
        visualTransformation = visualTransformation,

        // Keyboard
        keyboardOptions = if (isPassword)
            keyboardOptions.copy(keyboardType = KeyboardType.Password)
        else
            keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                isError = isError,
                label = label?.let { { Text(it, style = labelStyle) } },
                placeholder = placeholder?.let {
                    { Text(it, style = placeholderStyle, color = Grey500) }
                },
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
                prefix = prefix,
                suffix = suffix,
                supportingText = supportingText,
                colors = effectiveColors,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = effectiveColors,
                        shape = shape,
                        focusedBorderThickness = 1.2.dp,
                        unfocusedBorderThickness = 1.dp
                    )
                }
            )
        }
    )
}
