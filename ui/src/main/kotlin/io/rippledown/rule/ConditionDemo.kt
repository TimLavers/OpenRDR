package io.rippledown.rule

interface Handler {
    suspend fun tipForExpression(expression: String, context: List<String>): String
}

@Composable
fun ExpressionToConditionUI(handler: Handler) {
    var expression by remember { mutableStateOf("") }
    var conditionSyntax by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = expression,
            onValueChange = { expression = it },
            label = { Text("Enter Expression") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                runBlocking {
                    conditionSyntax = handler.api.tipForExpression(expression, listOf())
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Submit")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Condition Syntax:",
            style = MaterialTheme.typography.h6
        )
        Text(
            text = conditionSyntax,
            style = MaterialTheme.typography.body1
        )
    }
}