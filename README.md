# Gallant Connect — versão 1

Aplicativo Android de teste para a **Gallant Connect Max**, usando **Bluetooth Low Energy (BLE)** e o protocolo **FTMS (Fitness Machine Service)**.

## O que esta versão faz

- Procura dispositivos que anunciam o serviço FTMS (0x1826).
- Mostra os dispositivos encontrados.
- Conecta à bicicleta por Bluetooth LE.
- Descobre a característica **Indoor Bike Data (0x2AD2)**.
- Ativa notificações da bicicleta.
- Interpreta os campos disponíveis no pacote FTMS.
- Mostra:
  - Watts
  - RPM/cadência
  - Velocidade
  - Distância
  - Frequência cardíaca, se enviada pela bicicleta
  - Pacote FTMS bruto em hexadecimal para diagnóstico

> A disponibilidade de cada métrica depende do que a unidade da Gallant realmente transmite.

## Teste com a Gallant

1. Coloque uma bateria na bicicleta.
2. Abra o aplicativo.
3. Dê as permissões Bluetooth solicitadas.
4. Toque em **PROCURAR GALLANT**.
5. Procure o dispositivo, por exemplo `GLT2.5...`, ou o nome anunciado pela bicicleta.
6. Toque no dispositivo encontrado.
7. Aguarde a mensagem de conexão.
8. Comece a pedalar e observe os dados.

## Importante sobre o teste

Esta é uma primeira versão técnica para confirmar o pacote FTMS real transmitido pela bicicleta.

Se algum campo aparecer como `—`, isso pode significar que a bicicleta não está enviando esse campo naquele pacote. O campo **FTMS bruto** foi incluído para facilitar a análise do protocolo real.

## Compilação automática no GitHub Actions

O projeto inclui:

`.github/workflows/build-apk.yml`

No GitHub:

1. Abra a aba **Actions**.
2. Entre em **Build Gallant Connect APK**.
3. Aguarde a compilação.
4. Abra a execução concluída.
5. Na seção **Artifacts**, baixe `Gallant-Connect-debug`.

O arquivo gerado é:

`app-debug.apk`

Esta versão é de teste/debug. Para publicação na Google Play, será necessário configurar assinatura de release e as etapas de publicação.

## Estrutura

```text
ergometrica/
├── .github/workflows/build-apk.yml
├── app/
│   ├── src/main/java/com/golias349/gallantconnect/
│   │   ├── MainActivity.kt
│   │   ├── BleFtmsManager.kt
│   │   └── FtmsParser.kt
│   ├── src/main/res/layout/activity_main.xml
│   ├── src/main/res/values/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```
