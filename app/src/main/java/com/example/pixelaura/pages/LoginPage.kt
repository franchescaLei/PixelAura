package com.example.pixelaura.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pixelaura.R
import com.example.pixelaura.ui.theme.AuthViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogInScreen(navController: NavHostController, modifier: Modifier, authViewModel: AuthViewModel) {
    val errorMessage = remember { mutableStateOf("") }
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

        Text(
            text = "────────  Login  ────────",
            color = Color(0xFF780C28),
            fontSize = 20.sp,
            fontFamily = poppinsFont,
            style = TextStyle(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        // Username textfield
        val emailUsername = remember { mutableStateOf("") }
        val emailFocusReq = remember { FocusRequester() }
        val passwordFocusReq = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = emailUsername.value,
            onValueChange = { emailUsername.value = it },
            label = { Text("Email/Username", color = Color(0xFF780C28), fontFamily = poppinsFont) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusReq),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusReq.requestFocus() }
            ),
            shape = RoundedCornerShape(13.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF780C28),
                unfocusedBorderColor = Color(0xFF780C28)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        val password = remember { mutableStateOf("") }

        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password", color = Color(0xFF780C28), fontFamily = poppinsFont) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusReq),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(13.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF780C28),
                unfocusedBorderColor = Color(0xFF780C28)
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.value.isNotEmpty()) {
            Text(
                text = errorMessage.value,
                color = Color.Red,
                fontSize = 16.sp,
                fontFamily = poppinsFont
            )
        }
            Button(
                onClick = {
                    // Validate input fields
                    if (emailUsername.value.isEmpty() || password.value.isEmpty()) {
                        errorMessage.value = "Please fill in all fields."
                    } else {
                        authViewModel.login(emailUsername.value, password.value) { result ->
                            when (result) {
                                is AuthViewModel.Result.Success -> {
                                    navController.navigate("HomePage")
                                    errorMessage.value = "" // Clear any previous error messages
                                }
                                is AuthViewModel.Result.Failure -> {
                                    //other errors
                                    errorMessage.value = result.errorMessage
                                }
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF780C28)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text("Login", color = Color.White, fontFamily = poppinsFont, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }



        Spacer(modifier = Modifier.height(16.dp))

        // Sign-up text
        ClickableText(
            text = AnnotatedString.Builder().apply {
                append("Don't have an account yet? ")
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                append("Sign up here.")
                pop()
            }.toAnnotatedString(),
            onClick = {
                navController.navigate("signupPage")
            },
            style = TextStyle(
                color = Color(0xFF780C28),
                fontFamily = poppinsFont
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot Password
        ClickableText(
            text = AnnotatedString("Forgot Password"),
            onClick = {
                navController.navigate("forgotPasswordPage")
            },
            style = TextStyle(
                color = Color(0xFF780C28),
                textDecoration = TextDecoration.Underline,
                fontFamily = poppinsFont
            )
        )
    }
}
