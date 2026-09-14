# Third-Party Asset Ledger

FUI-11 does not vendor any third-party visual or font asset.

The Battle terminal treatment uses Android's platform `FontFamily.Monospace` through Compose. It
is not bundled as an external asset; the system-provided fallback remains available for devices
that do not expose a custom terminal font.

The project architecture records Material Symbols, Lucide, Kenney, Game-icons.net, Inter, and
Cinzel as possible future sources. None of those packages or font files are copied into the APK
by this phase, so no third-party attribution is currently required by FUI-11.

If a later phase adds a third-party asset, this ledger must record the local filename, source
project, original asset name, author where applicable, source URL, license, modifications, and
retrieval date before the asset is accepted.
