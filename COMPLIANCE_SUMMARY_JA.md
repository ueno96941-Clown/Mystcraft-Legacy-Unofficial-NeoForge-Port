# 公開配布コンプライアンス要約

**基準:** 2026-09-26 / `0.13.7.06-port.1.0.0-Ueno`

これは公開前の技術・文書監査の要約であり、法律上の助言ではありません。

- Mystcraft由来部分は、公式 `Mystcraft/Mystcraft-Legacy` の固定コミット `9bc8ddc061845df0cd5ea47f4fdef773cff786ae` を出所基準とし、その `LICENSE` は LGPL v3 です。
- 歴史的なCurseForge配布JAR、Minecraft/NeoForge本体JAR、Gradle配布物、開発キャッシュ、ログ、クラッシュレポート、私的比較資料は公開物へ含めません。
- PNG/OGGは120ファイルすべてを `ASSET_PROVENANCE_SHA256.tsv` で照合し、119ファイルが上流ソースと同一、1ファイル (`textures/item/white.png`) が移植側追加です。
- 公開版はMinecraft本体の改変済みクライアント/サーバーではなくMOD単体として配布し、公開ページとメタデータに独立した公開者 `ueno969` と連絡先 `ueno96941@gmail.com` を記載します。
- `Ueno` は非公式移植版を区別するためのmaintainer/build識別子で、Mystcraftの所有権・公式性を意味しません。
- Cyan由来の公式ゲーム音楽、ゲームバイナリ、公式マーケティング素材は今回の監査対象パッケージから確認されていません。
- Gradle Wrapper JARがソースZIPにない場合、固定したNeoForge MDKから復元し、Wrapper JARとGradle 9.2.1配布ZIPのSHA-256を検証します。
- 公開直前にはPublic Source監査、クリーンビルド、生成JAR監査、Minecraft/Cyanの現行ポリシー再確認を必須とします。

Minecraft表示:

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

Cyan表示:

**This product contains trademarks and/or copyrighted works of Cyan. All rights reserved by Cyan. This product is not official and is not endorsed by Cyan.**

詳細は `COMPLIANCE_AND_LICENSE_NOTICE.md` と `COMPLIANCE_AUDIT_REPORT.md` を参照してください。
