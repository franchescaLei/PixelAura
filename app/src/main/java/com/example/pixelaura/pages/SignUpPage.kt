package com.example.pixelaura.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pixelaura.R
import com.example.pixelaura.ui.theme.AuthViewModel
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavHostController, modifier: Modifier, authViewModel: AuthViewModel) {

    val emailSignUp = remember { mutableStateOf("") }
    val passwordSignUp = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }

    val emailSignUpFocusReq = remember { FocusRequester() }
    val passwordSignUpFocusReq = remember { FocusRequester() }
    val confirmPasswordFocusReq = remember { FocusRequester() }
    val focusManagerSignup = LocalFocusManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(45.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "PixelAura Logo",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "─────── Sign Up ──────",
            color = Color(0xFF780C28),
            fontSize = 20.sp,
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = emailSignUp.value,
            onValueChange = { emailSignUp.value = it },
            label = { Text("Enter Email", color = Color(0xFF780C28), fontFamily = poppinsFont) },
            modifier = Modifier.fillMaxWidth().focusRequester(emailSignUpFocusReq),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passwordSignUpFocusReq.requestFocus() }),
            shape = RoundedCornerShape(13.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF780C28),
                unfocusedBorderColor = Color(0xFF780C28)
            )
        )

        OutlinedTextField(
            value = passwordSignUp.value,
            onValueChange = { passwordSignUp.value = it },
            label = { Text("Enter Password", color = Color(0xFF780C28), fontFamily = poppinsFont) },
            modifier = Modifier.fillMaxWidth().focusRequester(passwordSignUpFocusReq),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusReq.requestFocus() }),
            shape = RoundedCornerShape(13.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF780C28),
                unfocusedBorderColor = Color(0xFF780C28)
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = { confirmPassword.value = it },
            label = { Text("Confirm Password", color = Color(0xFF780C28), fontFamily = poppinsFont) },
            modifier = Modifier.fillMaxWidth().focusRequester(confirmPasswordFocusReq),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManagerSignup.clearFocus() }),
            shape = RoundedCornerShape(13.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF780C28),
                unfocusedBorderColor = Color(0xFF780C28)
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(8.dp))
        if (errorMessage.value.isNotEmpty()) {
            Text(errorMessage.value, color = Color.Red, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Validate input fields
                if (emailSignUp.value.isEmpty() || passwordSignUp.value.isEmpty() || confirmPassword.value.isEmpty()) {
                    errorMessage.value = "Please fill in all fields."
                } else if (passwordSignUp.value.length < 6) {
                    errorMessage.value = "Password must be at least 6 characters long."
                } else if (passwordSignUp.value != confirmPassword.value) {
                    errorMessage.value = "Passwords don't match!"
                } else {
                    // Proceed with email sign-up
                    authViewModel.signUp(emailSignUp.value, passwordSignUp.value, confirmPassword.value) { result ->
                        when (result) {
                            is AuthViewModel.Result.Success -> {
                                // loginscreen
                                navController.navigate("LoginPage")
                                errorMessage.value = "Signed up Successfully!"
                            }
                            is AuthViewModel.Result.Failure -> {
                                // errors
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
            Text("Sign Up", color = Color.White, fontFamily = poppinsFont, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }


        Spacer(modifier = Modifier.height(16.dp))

        ClickableText(
            text = AnnotatedString.Builder().apply {
                append("Already have an account? ")
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                append("Log in here.")
                pop()
            }.toAnnotatedString(),
            onClick = {
                navController.popBackStack()
            },
            style = TextStyle(
                color = Color(0xFF780C28),
                fontFamily = poppinsFont
            )
        )
    }
}
