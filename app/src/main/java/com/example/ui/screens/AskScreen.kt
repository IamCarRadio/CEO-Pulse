package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AskChatMessage
import com.example.model.AskToolExecution
import com.example.model.FilterState
import com.example.model.UserProfile
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.RoleAllowedBg
import com.example.ui.theme.RoleAllowedGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLightContainer
import com.example.ui.theme.TealOnContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextHeadline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg

@Composable
fun AskScreen(
  user: UserProfile,
  isOwner: Boolean,
  messages: List<AskChatMessage>,
  isThinking: Boolean,
  starterQuestions: List<String>,
  filterState: FilterState,
  onSendMessage: (String) -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!isOwner) {
    OwnerAccessRestrictedView(user = user, modifier = modifier)
    return
  }

  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  // Auto-scroll to latest message
  LaunchedEffect(messages.size, isThinking) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundCanvas),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 960.dp)
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // 1. Executive Header
      AskAgentHeader(
        filterState = filterState,
        onClearHistory = onClearHistory
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Suggested Starter Questions (Chips)
      StarterQuestionsRow(
        questions = starterQuestions,
        onSelectQuestion = {
          onSendMessage(it)
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 3. Message Stream
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(messages, key = { it.id }) { message ->
          if (message.isUser) {
            UserMessageBubble(message = message)
          } else {
            AgentMessageBubble(message = message)
          }
        }

        if (isThinking) {
          item(key = "thinking_indicator") {
            AgentThinkingBubble()
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 4. Query Input Bar
      AskInputBar(
        inputText = inputText,
        onInputChange = { inputText = it },
        isThinking = isThinking,
        onSend = {
          if (inputText.isNotBlank()) {
            val q = inputText
            inputText = ""
            onSendMessage(q)
          }
        }
      )
    }
  }
}

/**
 * Access restricted view when non-owner attempts to access the Ask chat.
 */
@Composable
private fun OwnerAccessRestrictedView(
  user: UserProfile,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundCanvas)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SurfaceCard,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
      shadowElevation = 2.dp,
      modifier = Modifier
        .widthIn(max = 520.dp)
        .testTag("owner_restricted_card")
    ) {
      Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(WarningAmberBg),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Owner Only",
            tint = WarningAmber,
            modifier = Modifier.size(32.dp)
          )
        }

        Text(
          text = "Owner Access Only",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = TextHeadline
        )

        Text(
          text = "The Ask AI conversational data engine provides unconstrained executive strategic financial queries and is exclusively reserved for the Clinic Group Owner & CEO.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextBody,
          lineHeight = 22.sp
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SurfaceSubtle,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Current Account: ${user.email}",
              style = MaterialTheme.typography.labelMedium,
              color = TextMuted
            )
            Text(
              text = "Role: ${user.role}",
              style = MaterialTheme.typography.labelSmall,
              color = TextSubtle
            )
          }
        }

        Text(
          text = "Please log in with the verified Owner account to use the Ask Agent.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskAgentHeader(
  filterState: FilterState,
  onClearHistory: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = SurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TealLightContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Ask Agent",
            tint = TealPrimary,
            modifier = Modifier.size(20.dp)
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Ask Executive Agent",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
          ) {
            // Owner Verified Chip
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = RoleAllowedBg
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = RoleAllowedGreen,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "Owner Verified",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = RoleAllowedGreen,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            // Read-Only Chip
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = SurfaceSubtle
            ) {
              Text(
                text = "Read-Only Mode",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted,
                fontWeight = FontWeight.Medium
              )
            }
          }

          Text(
            text = "Context: ${filterState.selectedBranch} • ${filterState.formattedDateDisplay}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextMuted
          )
        }
      }

      IconButton(
        onClick = onClearHistory,
        modifier = Modifier
          .size(48.dp)
          .testTag("ask_clear_button")
      ) {
        Icon(
          imageVector = Icons.Default.DeleteSweep,
          contentDescription = "Clear Chat History",
          tint = TextMuted,
          modifier = Modifier.size(22.dp)
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StarterQuestionsRow(
  questions: List<String>,
  onSelectQuestion: (String) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = "SUGGESTED STARTER QUESTIONS",
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
      fontWeight = FontWeight.SemiBold,
      color = TextMuted,
      modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      questions.forEachIndexed { index, question ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelectQuestion(question) }
            .testTag("ask_starter_chip_$index")
        ) {
          Row(
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = TealAccent,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = question,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = TextBody,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

@Composable
private fun UserMessageBubble(message: AskChatMessage) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .background(TealPrimary)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Text(
        text = message.text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White
      )
    }
  }
}

@Composable
private fun AgentMessageBubble(message: AskChatMessage) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Start
  ) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(TealLightContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = "Agent",
        tint = TealPrimary,
        modifier = Modifier.size(18.dp)
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Column(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .background(SurfaceCard)
        .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Tool executions cards
      if (message.toolCalls.isNotEmpty()) {
        message.toolCalls.forEach { toolExec ->
          ToolExecutionCard(toolExec = toolExec)
        }
      }

      // Main message text
      Text(
        text = message.text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextHeadline,
        lineHeight = 22.sp
      )

      // Math working card if present
      if (!message.mathWorking.isNullOrBlank()) {
        MathWorkingCard(mathWorking = message.mathWorking)
      }
    }
  }
}

@Composable
private fun ToolExecutionCard(toolExec: AskToolExecution) {
  var isExpanded by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = SurfaceSubtle,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(13.dp)
          )

          Text(
            text = "Tool Executed: ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = TealLightContainer
          ) {
            Text(
              text = toolExec.toolName,
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = TealOnContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clickable { isExpanded = !isExpanded }
            .padding(4.dp)
        ) {
          Text(
            text = if (isExpanded) "Hide Data" else "Inspect Data",
            style = MaterialTheme.typography.labelSmall,
            color = TealPrimary,
            fontWeight = FontWeight.Medium
          )
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Filters used display
      Text(
        text = "Filters: ${toolExec.filtersUsedDisplay}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextBody
      )

      // Collapsible raw JSON / output view
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
            .padding(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Code,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(12.dp)
            )
            Text(
              text = "Raw Verified Metrics Output",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = Color(0xFF94A3B8)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = toolExec.rawOutputJson,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              color = Color(0xFF38BDF8)
            )
          )
        }
      }
    }
  }
}

@Composable
private fun MathWorkingCard(mathWorking: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFF0FDF4),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Calculate,
          contentDescription = null,
          tint = RoleAllowedGreen,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "Math Working (Formulas & Steps)",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = RoleAllowedGreen
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = mathWorking,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 11.5.sp,
          lineHeight = 18.sp
        ),
        color = Color(0xFF14532D)
      )
    }
  }
}

@Composable
private fun AgentThinkingBubble() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(TealLightContainer),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = TealPrimary
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = SurfaceCard,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Executing tool query & computing verified metrics...",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )
      }
    }
  }
}

@Composable
private fun AskInputBar(
  inputText: String,
  onInputChange: (String) -> Unit,
  isThinking: Boolean,
  onSend: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = SurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .imePadding(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = onInputChange,
        placeholder = {
          Text(
            text = "Ask a data question (e.g., 'What is today's revenue across all branches?')...",
            style = MaterialTheme.typography.bodySmall,
            color = TextSubtle
          )
        },
        modifier = Modifier
          .weight(1f)
          .testTag("ask_input_field"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (!isThinking) onSend() }),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = TealPrimary,
          unfocusedBorderColor = BorderSubtle,
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent
        )
      )

      Button(
        onClick = onSend,
        enabled = inputText.isNotBlank() && !isThinking,
        colors = ButtonDefaults.buttonColors(
          containerColor = TealPrimary,
          disabledContainerColor = BorderSubtle
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.testTag("ask_send_button")
      ) {
        if (isThinking) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = Color.White
          )
        } else {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
