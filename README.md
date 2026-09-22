# Gallant Connect — versão 1

Projeto Android para a Gallant Connect Max com Bluetooth LE / FTMS.

## O que esta versão faz

- Procura dispositivos que anunciam o serviço FTMS.
- Conecta à bicicleta.
- Lê a característica FTMS Indoor Bike Data (0x2AD2).
- Mostra:
  - Watts
  - RPM/cadência
  - velocidade
  - distância
  - frequência cardíaca, se a bicicleta enviar
  - calorias, se a bicicleta enviar
- Possui botão para solicitar início/retomada do treino quando o equipamento aceita o comando FTMS.

## Como abrir

1. Instale o Android Studio.
2. Abra a pasta `GallantConnect`.
3. Espere o Gradle sincronizar.
4. Conecte o celular por USB.
5. Ative as opções de desenvolvedor/depuração USB no celular.
6. Execute o projeto no aparelho.

## Teste com a Gallant

1. Coloque a bateria da bicicleta.
2. Abra o app.
3. Dê permissão ao Bluetooth.
4. Toque em `PROCURAR GALLANT`.
5. Procure `GLT2.5...` ou o nome da bicicleta.
6. Toque no dispositivo.
7. Aguarde `Pronto. Comece a pedalar.`
8. Pedale e observe watts/RPM/km/h.

## Observação

A Gallant Connect Max é anunciada pelo fabricante com Bluetooth 4.0 e métricas como distância, tempo, calorias, resistência, frequência cardíaca, cadência e potência. A disponibilidade de cada campo depende do que a bicicleta realmente transmite por FTMS.

Esta é uma primeira versão de teste; o objetivo é confirmar o pacote FTMS real transmitido pela sua unidade antes de adicionar histórico, gráficos, metas e outras funções.

## Compilação automática no GitHub Actions

O projeto inclui `.github/workflows/build-apk.yml`. Depois de subir o projeto para um repositório GitHub, abra **Actions**, execute **Build Gallant Connect APK** (ou faça push na `main`/`master`) e, ao terminar, baixe o artefato **Gallant-Connect-debug**. Dentro dele estará `app-debug.apk`.

Esta é uma versão de teste/depuração. Para publicar na Google Play, será necessário configurar assinatura de release.
