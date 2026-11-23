# Spotifusion

<p align="center">
  <img src="docs/brand/myspoti-logo-512.png" alt="Logo do Spotifusion" width="120">
</p>

Cliente de música para Android com interface nativa em Kotlin e Jetpack Compose, integração com o Spotify Web Player, letras sincronizadas e recursos de reprodução local.

Este repositório é mantido por **[davidkalil10](https://github.com/davidkalil10)**. É um fork do **[Spotifusion de p1di](https://github.com/p1di/spotifusion)**, construído sobre a base do **[Myspoti / Spotui de kota-zo e MetrolistGroup](https://github.com/kota-zo/myspoti)**. Os créditos e as licenças dos projetos de origem são preservados.

[Versões e downloads](https://github.com/davidkalil10/spotifusion/releases) · [Relatar um problema](https://github.com/davidkalil10/spotifusion/issues) · [Histórico de alterações](CHANGELOG.md)

## Alterações deste fork

- Login pela página oficial do Spotify em uma WebView móvel separada, com as configurações padrão do Android. Essa abordagem resolveu o erro HTTP 400 (`internal_error`) observado ao solicitar o código por e-mail no login com a configuração de navegador de computador.
- Compartilhamento da sessão pelo armazenamento de cookies do Android e confirmação da autenticação antes de abrir a interface do app.
- Remoção do botão flutuante de debug e dos botões temporários de teste na tela de login.
- Diagnóstico de falhas de login com filtragem de dados pessoais. A exportação de relatórios continua disponível nas configurações.

## Recursos

- Catálogo, busca, biblioteca, playlists, álbuns e artistas.
- Reprodução pelo Spotify Web Player integrado e de músicas locais.
- Letras sincronizadas com integração ao LRCLIB.
- Cache de reprodução e downloads conforme as fontes disponíveis no projeto.
- Controles na tela de bloqueio, notificações, Bluetooth e Android Auto.

## Compilar

Requisitos: **JDK 21**, Android SDK com a plataforma **37.0** (`compileSdk = 37`) e ferramentas de compilação compatíveis. O app requer Android 10 ou superior (`minSdk = 29`).

```bash
git clone https://github.com/davidkalil10/spotifusion.git
cd spotifusion/base
./gradlew assembleDebug
```

No Windows, use `gradlew.bat assembleDebug` na pasta `base`.

APK gerado: `base/app/build/outputs/apk/debug/app-debug.apk`.

Para executar as verificações do login:

```bash
# Na pasta base
./gradlew :host-android:testDebugUnitTest

# Na raiz do repositório, com Node.js instalado
node --test tools/test-login-diagnostics.cjs
```

## Estrutura

| Módulo | Responsabilidade |
| --- | --- |
| `base/app` | Interface, navegação, preferências e recursos locais. |
| `base/api` | Modelos e contratos da aplicação. |
| `base/api-spotify` | Integração com os serviços e o Web Player do Spotify. |
| `base/host-android` | Login móvel, WebView do player e integração com o Android. |
| `base/host-cdp` | Ferramentas de validação do Web Player em ambiente desktop. |

## Créditos

- **[davidkalil10](https://github.com/davidkalil10)** — manutenção deste fork, integração da correção do login móvel e ajustes da interface de diagnóstico.
- **[p1di / Spotifusion](https://github.com/p1di/spotifusion)** — projeto do qual este repositório foi diretamente derivado, com suas adaptações de interface e reprodução.
- **[kota-zo, MetrolistGroup / Myspoti e Spotui](https://github.com/kota-zo/myspoti)** — base original da arquitetura, interface e integração com o Spotify Web Player.
- **[SpotiDuck](https://github.com/SpotiDuck)** — referências de interface e otimizações citadas pelo projeto de origem.
- **[BitChord](https://github.com/kushagrasinghx/BitChord)** — componentes de correspondência de faixas e downloads incorporados pelo projeto de origem.
- **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)** — extração de informações de mídia.
- **[LRCLIB](https://lrclib.net)** — serviço de letras sincronizadas.
- **[Material Design Icons](https://github.com/google/material-design-icons)** — ícones utilizados na interface.

As atribuições e licenças de terceiros estão em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Licença e vínculo com o Spotify

Distribuído sob a **GNU General Public License v3.0**. Consulte [LICENSE](LICENSE).

Este projeto é independente e não é afiliado, patrocinado ou endossado pelo Spotify. Spotify é uma marca da Spotify AB.
