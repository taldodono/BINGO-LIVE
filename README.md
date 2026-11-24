# BINGO LIVE — Monitor de Apostas (MVP gratuito)

## Estado desta entrega
Projeto-fonte Android Studio (Kotlin + Jetpack Compose). **Não é um APK compilado**.
Este ambiente não executou Gradle/Android SDK; portanto o APK ainda precisa ser compilado e testado num ambiente Android Studio.

## Funcionalidades iniciais
- Tema escuro e painel básico.
- Cadastro local de bilhetes (um pick por bilhete nesta primeira estrutura).
- Esportes: NBA, Futebol, Tênis e Vôlei.
- Armazenamento local no aparelho via SharedPreferences.
- Motor inicial de liquidação numérica para mercados de over/under.
- Mercados não reconhecidos não devem ser liquidados automaticamente; ficam pendentes/aguardando confirmação.

## Limitações importantes
- Ainda não há feed de placares ao vivo/API.
- OCR está listado como dependência ML Kit, mas a captura/seleção de imagem e extração ainda não foi conectada.
- Notificações e atualização em segundo plano ainda não estão implementadas.
- A liquidação automática está em protótipo e não deve ser usada para decidir pagamentos reais.
- Para produção, cada mercado precisa de regra explícita, período correto, prorrogação quando aplicável, regras da casa e fonte oficial/confiável.

## Como abrir e compilar
1. Instale Android Studio (versão recente) com Android SDK 35.
2. Abra a pasta `BINGO_LIVE_Android_MVP`.
3. Aguarde a sincronização do Gradle.
4. Conecte um Android ou inicie um emulador.
5. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Próximas etapas do MVP
1. Corrigir e ampliar o modelo para múltiplas seleções por bilhete.
2. Adicionar importação de imagem e OCR ML Kit.
3. Integrar dados gratuitos autorizados e cache local.
4. Implementar serviço de notificações e sincronização.
5. Validar regras de liquidação para cada mercado dos quatro esportes.
