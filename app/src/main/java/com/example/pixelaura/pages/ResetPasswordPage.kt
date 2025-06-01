package com.example.pixelaura.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pixelaura.R
import com.example.pixelaura.ui.theme.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordPage(navController: NavHostController, modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val email = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }
    val successMessage = remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(45.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "PixelAura Logo",
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Title Text
        Text(
            text = "─── Reset Your Password ───",
            color = Color(0xFF780C28),
            fontSize = 20.sp,
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email Input Field
        val email = remember { mutableStateOf("") }
        val emailfFocusReq = remember { FocusRequester() }
        val codeFocusReq = remember { FocusRequester() }
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Enter email", color = Color(0xFF780C28), fontFamily = poppinsFont) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailfFocusReq),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { codeFocusReq.requestFocus() }
            ),
            shape = RoundedCornerShape(13.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF780C28),
                unfocusedBorderColor = Color(0xFF780C28)
            )
        )


        Spacer(modifier = Modifier.height(16.dp))

        // Error and Success Messages
        if (errorMessage.value.isNotEmpty()) {
            Text(
                text = errorMessage.value,
                color = Color.Red,
                fontFamily = poppinsFont,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Password Button
        Button(
            onClick = {
                authViewModel.resetPassword(email.value) { result ->
                    when (result) {
                        is AuthViewModel.Result.Success -> {
                            Toast.makeText(context, "Please check your email and log in with your new password.", Toast.LENGTH_LONG).show()
                            errorMessage.value = ""
                        }
                        is AuthViewModel.Result.Failure -> {
                            errorMessage.value = result.errorMessage
                            successMessage.value = ""
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Reset Password",
                color = Color.White,
                fontFamily = poppinsFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }




        Spacer(modifier = Modifier.height(16.dp))

        ClickableText(
            text = AnnotatedString("Back to Login"),
            onClick = {
                navController.popBackStack()
            },
            style = TextStyle(
                color = Color(0xFF780C28),
                fontFamily = poppinsFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
            )
        )
    }
}

