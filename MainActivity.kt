package com.bingolive.monitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Bg = Color(0xFF090D14)
private val Panel = Color(0xFF151D29)
private val Green = Color(0xFF35D07F)
private val Red = Color(0xFFFF5B68)
private val Muted = Color(0xFF9AA8BA)

data class Pick(
    val sport: String, val event: String, val market: String,
    val selection: String, val target: Double, val result: Double,
    val resultReady: Boolean, val status: String
)
data class Ticket(val id: String, val title: String, val stake: Double, val odds: Double, val picks: List<Pick>, val created: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel()
        setContent { BingoApp(this) }
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("bingo_updates", "Atualizações BINGO LIVE", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

private fun encode(tickets: List<Ticket>): String {
    val arr = JSONArray()
    tickets.forEach { t ->
        val o = JSONObject().put("id", t.id).put("title", t.title).put("stake", t.stake).put("odds", t.odds).put("created", t.created)
        val ps = JSONArray()
        t.picks.forEach { p -> ps.put(JSONObject().put("sport",p.sport).put("event",p.event).put("market",p.market).put("selection",p.selection).put("target",p.target).put("result",p.result).put("ready",p.resultReady).put("status",p.status)) }
        o.put("picks", ps); arr.put(o)
    }
    return arr.toString()
}
private fun decode(raw: String): List<Ticket> = try {
    val a = JSONArray(raw); (0 until a.length()).map { i ->
        val o = a.getJSONObject(i); val p = o.getJSONArray("picks")
        Ticket(o.getString("id"),o.getString("title"),o.getDouble("stake"),o.getDouble("odds"),
            (0 until p.length()).map { j -> val x=p.getJSONObject(j); Pick(x.getString("sport"),x.getString("event"),x.getString("market"),x.getString("selection"),x.getDouble("target"),x.getDouble("result"),x.getBoolean("ready"),x.getString("status")) },o.getLong("created"))
    }
} catch (_: Exception) { emptyList() }

private fun settle(p: Pick): String {
    if (!p.resultReady) return "PENDENTE"
    val m = p.market.lowercase()
    val won = when {
        m.contains("mais de") || m.contains("over") || m.contains("acima") -> p.result > p.target
        m.contains("menos de") || m.contains("under") || m.contains("abaixo") -> p.result < p.target
        m.contains("vencedor") || m.contains("moneyline") || m.contains("1x2") -> p.selection.equals("casa",true) && p.result > 0 || p.selection.equals("fora",true) && p.result < 0 || p.selection.equals("empate",true) && p.result == 0
        m.contains("handicap") -> (p.result + p.target) > 0
        else -> return "AGUARDANDO CONFIRMAÇÃO"
    }
    return if (won) "GREEN" else "RED"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BingoApp(ctx: Context) {
    val prefs = remember { ctx.getSharedPreferences("bingo_live", Context.MODE_PRIVATE) }
    var tickets by remember { mutableStateOf(decode(prefs.getString("tickets","[]") ?: "[]")) }
    var page by remember { mutableStateOf("home") }
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Ticket?>(null) }
    fun save(list: List<Ticket>) { tickets=list; prefs.edit().putString("tickets",encode(list)).apply() }

    MaterialTheme(colorScheme = darkColorScheme(primary=Green, background=Bg, surface=Panel, onBackground=Color.White, onSurface=Color.White)) {
        Scaffold(containerColor=Bg, topBar={
            TopAppBar(title={ Column { Text("BINGO LIVE", fontWeight=FontWeight.Black); Text("MONITOR DE APOSTAS • GRATUITO", style=MaterialTheme.typography.labelSmall, color=Muted) } },
                colors=TopAppBarDefaults.topAppBarColors(containerColor=Bg, titleContentColor=Color.White),
                actions={ IconButton(onClick={page="home"}) { Icon(Icons.Default.Home,null) }; IconButton(onClick={showAdd=true}) { Icon(Icons.Default.Add,null) } })
        }, floatingActionButton={ FloatingActionButton(onClick={showAdd=true},containerColor=Green,contentColor=Bg){ Icon(Icons.Default.Add,"Novo bilhete") } }) { pad ->
            Column(Modifier.padding(pad).fillMaxSize().padding(horizontal=16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Stat("BILHETES",tickets.size.toString(), Modifier.weight(1f))
                    Stat("GREEN",tickets.sumOf { t -> t.picks.count { it.status=="GREEN" } }.toString(), Modifier.weight(1f))
                    Stat("RED",tickets.sumOf { t -> t.picks.count { it.status=="RED" } }.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                if (selected != null) {
                    TicketDetail(selected!!, onBack={selected=null}, onSave={updated -> save(tickets.map { if(it.id==updated.id) updated else it }); selected=updated})
                } else {
                    Text(if(page=="home") "Seus bilhetes" else "Painel", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (tickets.isEmpty()) {
                        Card(colors=CardDefaults.cardColors(containerColor=Panel), modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ConfirmationNumber,null,tint=Green)
                                Text("Nenhum bilhete cadastrado",fontWeight=FontWeight.Bold)
                                Text("Toque no botão + para adicionar seu primeiro bilhete.",color=Muted)
                                Text("A versão de teste guarda os dados neste aparelho.",color=Muted,style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        items(tickets, key={it.id}) { t ->
                            Card(Modifier.fillMaxWidth().clickable { selected=t }, colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(14.dp)) {
                                Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                    Row(verticalAlignment=Alignment.CenterVertically) { Text(t.title,Modifier.weight(1f),fontWeight=FontWeight.Bold); Text("ODD ${"%.2f".format(t.odds)}",color=Green,fontWeight=FontWeight.Bold) }
                                    Text("${t.picks.size} seleções • Stake R$ ${"%.2f".format(t.stake)}",color=Muted)
                                    Text(t.picks.groupBy { it.status }.entries.joinToString("  ") { "${it.key}: ${it.value.size}" },color=Muted,style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("A liquidação automática depende de dados finais confiáveis. Mercados não reconhecidos ficam em AGUARDANDO CONFIRMAÇÃO.",color=Muted,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        if(showAdd) AddTicketDialog(onDismiss={showAdd=false}, onAdd={t -> save(tickets + t); showAdd=false})
    }
}

@Composable
fun Stat(label:String,value:String) {
    Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.padding(12.dp)) { Text(label,color=Muted,style=MaterialTheme.typography.labelSmall); Text(value,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall) }
    }
}

@Composable
fun AddTicketDialog(onDismiss:()->Unit,onAdd:(Ticket)->Unit) {
    var title by remember { mutableStateOf("Meu bilhete") }
    var sport by remember { mutableStateOf("Futebol") }
    var event by remember { mutableStateOf("") }
    var market by remember { mutableStateOf("Mais de") }
    var selection by remember { mutableStateOf("Mais de 2.5") }
    var target by remember { mutableStateOf("2.5") }
    var stake by remember { mutableStateOf("5") }
    var odds by remember { mutableStateOf("2.0") }
    AlertDialog(onDismissRequest=onDismiss, containerColor=Panel, title={Text("Novo bilhete")}, text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title,{title=it},label={Text("Nome do bilhete")},singleLine=true)
            Text("Esporte")
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { listOf("NBA","Futebol","Tênis","Vôlei").forEach { FilterChip(selected=sport==it,onClick={sport=it},label={Text(it)}) } }
            OutlinedTextField(event,{event=it},label={Text("Partida / evento")},singleLine=true)
            OutlinedTextField(market,{market=it},label={Text("Mercado (ex.: Mais de)")},singleLine=true)
            OutlinedTextField(selection,{selection=it},label={Text("Seleção")},singleLine=true)
            OutlinedTextField(target,{target=it},label={Text("Linha/limite numérico")},singleLine=true)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(stake,{stake=it},label={Text("Stake R$")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                OutlinedTextField(odds,{odds=it},label={Text("Odd")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
            }
            Text("O resultado final poderá ser informado na tela do bilhete.",color=Muted,style=MaterialTheme.typography.bodySmall)
        }
    }, confirmButton={ TextButton(onClick={
        val t=Ticket(UUID.randomUUID().toString(),title,stake.replace(",",".").toDoubleOrNull()?:0.0,odds.replace(",",".").toDoubleOrNull()?:1.0,
            listOf(Pick(sport,event,market,selection,target.replace(",",".").toDoubleOrNull()?:0.0,0.0,false,"PENDENTE")),System.currentTimeMillis())
        onAdd(t)
    }) { Text("Adicionar",color=Green) } }, dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable
fun TicketDetail(ticket:Ticket,onBack:()->Unit,onSave:(Ticket)->Unit) {
    var result by remember(ticket.id) { mutableStateOf("") }
    val pick=ticket.picks.first()
    val status=settle(pick)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        TextButton(onClick=onBack){Text("← Voltar")}
        Text(ticket.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text("${pick.sport} • ${pick.event}",color=Muted)
        Text("Mercado: ${pick.market} | Seleção: ${pick.selection}")
        Text("Status: ${if(pick.resultReady) pick.status else "PENDENTE"}",color=if(pick.status=="GREEN") Green else if(pick.status=="RED") Red else Muted,fontWeight=FontWeight.Bold)
        OutlinedTextField(result,{result=it},label={Text("Resultado final numérico (ex.: total de pontos/gols)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
        Button(onClick={
            val value=result.replace(",",".").toDoubleOrNull()
            if(value!=null) {
                val updatedPick=pick.copy(result=value,resultReady=true,status=settle(pick.copy(result=value,resultReady=true)))
                onSave(ticket.copy(picks=listOf(updatedPick)))
            }
        },modifier=Modifier.fillMaxWidth()) { Text("Liquidar automaticamente") }
        Text("Para vencedor/1X2 e handicaps complexos, esta versão exige configuração específica e pode manter a seleção aguardando confirmação.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
}
