package com.runanywhere.startup_hackathon20

import android.app.Application
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.runanywhere.startup_hackathon20.ui.theme.EventPlannerTheme
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventPlannerTheme {
                val eventViewModel: EventViewModel = viewModel(factory = EventViewModelFactory(application))
                val chatViewModel: ChatViewModel = viewModel()
                val loginViewModel: LoginViewModel = viewModel()
                EventPlannerApp(eventViewModel, chatViewModel, loginViewModel)
            }
        }
    }
}

@Composable
fun EventPlannerApp(
    eventViewModel: EventViewModel,
    chatViewModel: ChatViewModel,
    loginViewModel: LoginViewModel
) {
    val navController = rememberNavController()
    val loginState by loginViewModel.loginState
    val signUpState by loginViewModel.signUpState
    val scope = rememberCoroutineScope()
    val events by eventViewModel.events.observeAsState(initial = emptyList())

    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                loginState = loginState,
                onLoginClicked = { email, password ->
                    scope.launch {
                        loginViewModel.login(email, password)
                    }
                },
                onSignUpClicked = { navController.navigate("signup") }
            )

            LaunchedEffect(loginState) {
                if (loginState is LoginState.Success) {
                    navController.navigate("event_list") {
                        popUpTo("login") { inclusive = true }
                    }
                    loginViewModel.resetLoginState() // Reset state after navigation
                }
            }
        }
        composable("signup") {
            SignUpScreen(
                signUpState = signUpState,
                onSignUpClicked = { email, password ->
                    scope.launch {
                        loginViewModel.signUp(email, password)
                    }
                },
                onBackToLoginClicked = {
                    navController.popBackStack()
                    loginViewModel.resetSignUpState()
                }
            )
            LaunchedEffect(signUpState) {
                if (signUpState is SignUpState.Success) {
                    // Navigate back to login after a successful sign-up
                    navController.popBackStack()
                    loginViewModel.resetSignUpState()
                }
            }
        }
        composable("event_list") {
            EventListScreen(
                events = events,
                onEventClick = { eventId ->
                    navController.navigate("event_detail/$eventId")
                },
                onAddEvent = { navController.navigate("add_event") },
                onSignOut = {
                    loginViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("event_list") { inclusive = true }
                    }
                }
            )
        }

        composable("add_event") {
            AddEventScreen(
                onEventCreated = { event ->
                    eventViewModel.addEvent(event)
                    navController.popBackStack()
                }
            )
        }

        composable(
            "event_detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
            val context = LocalContext.current
            EventDetailScreen(
                event = events.find { it.id == eventId },
                tasks = eventViewModel.getTasksForEvent(eventId),
                guests = eventViewModel.getGuestsForEvent(eventId),
                expenses = eventViewModel.getExpensesForEvent(eventId),
                onToggleTask = { taskId -> eventViewModel.toggleTask(eventId, taskId) },
                onAddGuest = { guest -> eventViewModel.addGuest(eventId, guest) },
                onUpdateRsvp = { guestId, status -> eventViewModel.updateGuestRsvp(eventId, guestId, status) },
                onAddExpense = { expense -> eventViewModel.addExpense(eventId, expense) },
                onScheduleReminder = { event -> eventViewModel.scheduleReminder(context, event) },
                chatViewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    events: List<Event>,
    onEventClick: (Long) -> Unit,
    onAddEvent: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Events") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ExitToApp, "Sign Out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEvent) {
                Icon(Icons.Default.Add, "Add Event")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(events) { event ->
                EventCard(event = event, onClick = { onEventClick(event.id) })
            }
        }
    }
}

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            if (event.imageUri != null) {
                AsyncImage(
                    model = event.imageUri,
                    contentDescription = "Event Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(event.name, style = MaterialTheme.typography.headlineSmall)
                Text("${event.type} • ${event.date}", style = MaterialTheme.typography.bodyMedium)
                if (event.budget.isNotEmpty()) {
                    Text("Budget: ₹${event.budget}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(onEventCreated: (Event) -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(EventType.BIRTHDAY) }
    var budget by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            date = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
        }
    )

    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState())) {
        Text("Add Event Details", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Select Image")
        }
        imageUri?.let {
            AsyncImage(
                model = it,
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 8.dp),
                contentScale = ContentScale.Crop
            )
        }
        TextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date",
                    modifier = Modifier.clickable { datePickerDialog.show() }
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Event Type:")
        EventType.entries.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                Text(type.name)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = budget, onValueChange = { budget = it }, label = { Text("Budget (₹)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            TextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start Time") }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            TextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End Time") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotEmpty() && date.isNotEmpty()) {
                    onEventCreated(Event(
                        name = name,
                        date = date,
                        type = selectedType,
                        budget = budget,
                        location = location,
                        startTime = startTime,
                        endTime = endTime,
                        description = description,
                        imageUri = imageUri?.toString()
                    ))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Event")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event?,
    tasks: List<Task>,
    guests: List<Guest>,
    expenses: List<Expense>,
    onToggleTask: (String) -> Unit,
    onAddGuest: (Guest) -> Unit,
    onUpdateRsvp: (String, RSVPStatus) -> Unit,
    onAddExpense: (Expense) -> Unit,
    onScheduleReminder: (Event) -> Unit,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit
) {
    if (event == null) return

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onScheduleReminder(event) }) {
                        Icon(Icons.Default.Notifications, "Schedule Reminder")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Details") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tasks") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Guests") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Budget") })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("AI Assistant") })
            }

            when (selectedTab) {
                0 -> EventDetailsTab(event = event)
                1 -> TaskListTab(tasks = tasks, onToggleTask = onToggleTask)
                2 -> GuestListTab(guests = guests, onAddGuest = onAddGuest, onUpdateRsvp = onUpdateRsvp)
                3 -> BudgetTab(event.budget, expenses, onAddExpense)
                4 -> ChatTab(event = event, chatViewModel = chatViewModel)
            }
        }
    }
}

@Composable
fun EventDetailsTab(event: Event) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        if (event.imageUri != null) {
            AsyncImage(
                model = event.imageUri,
                contentDescription = "Event Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Type: ${event.type}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Date: ${event.date}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Time: ${event.startTime} - ${event.endTime}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Location: ${event.location}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Budget: ₹${event.budget}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Description:", style = MaterialTheme.typography.headlineSmall)
        Text(event.description, style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
fun TaskListTab(tasks: List<Task>, onToggleTask: (String) -> Unit) {
    LazyColumn {
        items(tasks) { task ->
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask(task.id) })
                Column {
                    Text(task.title, style = if (task.isCompleted) MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough) else MaterialTheme.typography.bodyMedium)
                    Text(task.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestListTab(guests: List<Guest>, onAddGuest: (Guest) -> Unit, onUpdateRsvp: (String, RSVPStatus) -> Unit) {
    var showAddGuestDialog by remember { mutableStateOf(false) }

    if (showAddGuestDialog) {
        AddGuestDialog(
            onAddGuest = {
                onAddGuest(it)
                showAddGuestDialog = false
            },
            onDismiss = { showAddGuestDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddGuestDialog = true }) {
                Icon(Icons.Default.Add, "Add Guest")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(guests) { guest ->
                GuestCard(guest = guest, onUpdateRsvp = { onUpdateRsvp(guest.id, it) })
            }
        }
    }
}

@Composable
fun GuestCard(guest: Guest, onUpdateRsvp: (RSVPStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(guest.name, style = MaterialTheme.typography.headlineSmall)
            Text(guest.email, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RSVP: ", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { expanded = true }) {
                    Text(guest.rsvpStatus.name)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    RSVPStatus.entries.forEach { status ->
                        DropdownMenuItem(text = { Text(status.name) }, onClick = {
                            onUpdateRsvp(status)
                            expanded = false
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGuestDialog(onAddGuest: (Guest) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Guest") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && email.isNotEmpty()) {
                    onAddGuest(Guest(name = name, email = email))
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTab(totalBudget: String, expenses: List<Expense>, onAddExpense: (Expense) -> Unit) {
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onAddExpense = {
                onAddExpense(it)
                showAddExpenseDialog = false
            },
            onDismiss = { showAddExpenseDialog = false }
        )
    }

    val totalSpent = expenses.sumOf { it.cost }
    val remaining = totalBudget.toDoubleOrNull()?.minus(totalSpent) ?: 0.0

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExpenseDialog = true }) {
                Icon(Icons.Default.Add, "Add Expense")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Total Budget: ₹$totalBudget", style = MaterialTheme.typography.headlineSmall)
            Text("Total Spent: ₹$totalSpent", style = MaterialTheme.typography.bodyLarge)
            Text("Remaining: ₹$remaining", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = if (totalBudget.toDoubleOrNull() ?: 0.0 > 0) (totalSpent / (totalBudget.toDoubleOrNull() ?: 1.0)).toFloat() else 0f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(expenses) { expense ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(expense.name)
                        Text("₹${expense.cost}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(onAddExpense: (Expense) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Expense Name") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && cost.isNotEmpty()) {
                    onAddExpense(Expense(name = name, cost = cost.toDouble()))
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTab(event: Event, chatViewModel: ChatViewModel) {
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    var userInput by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }



        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f),
                label = { Text("Ask for suggestions...") },
                enabled = !isLoading,
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        chatViewModel.sendMessageWithContext(userInput, event)
                        userInput = ""
                    }
                },
                enabled = !isLoading
            ) {
                Text("Send")
            }
        }
    }
}
