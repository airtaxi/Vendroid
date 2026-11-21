# Vendroid

[Vencord/Vendroid](https://github.com/Vencord/Vendroid)의 포크로, [AITranslate](https://github.com/airtaxi/Vencord-AITranslate) 플러그인을 포함한 Vencord를 불러오는 안드로이드용 디스코드 클라이언트입니다. 데스크톱과 같은 디스코드 웹 앱을 WebView로 열고 Vencord를 주입합니다.

모바일에서 디스코드 웹 앱은 공식 앱보다 느립니다.

| | |
|:--:|:--:|
|![image](https://github.com/Vencord/Vendroid/assets/45497981/e6464167-78b1-4f38-8e96-bb355ea5bbc3)|![image](https://github.com/Vencord/Vendroid/assets/45497981/3f6b278e-f18d-4cae-964f-f357f06ca2bd)|

## 원본과 다른 점

- Vencord 공식 개발 빌드 대신, 이 저장소의 [`vencord-build` 릴리스](https://github.com/airtaxi/Vendroid/releases/tag/vencord-build)에 올린 Vencord 웹 빌드(`browser.js`, `browser.css`)를 내려받습니다. 이 빌드에는 AITranslate 플러그인이 들어 있습니다.
- 페이지 스크립트에서 `VencordMobile.nativeFetch(url, { method, headers, body })`를 호출하면 HTTPS 요청을 WebView가 아닌 앱에서 보냅니다. WebView의 CORS 검사를 거치지 않으므로, CORS를 허용하지 않는 API 서버도 호출할 수 있습니다. 결과는 `{ status, data }` 형태로 돌아오며, 요청을 보내지 못하면 `status`가 `-1`입니다.

## 빌드

JDK 17 이상과 Android SDK가 필요합니다. 서명용 키 저장소를 지정해서 릴리스 APK를 빌드합니다.

```sh
./gradlew app:assembleRelease \
  -Pandroid.injected.signing.store.file="<키 저장소 경로>" \
  -Pandroid.injected.signing.store.password="<키 저장소 비밀번호>" \
  -Pandroid.injected.signing.key.alias="<키 별칭>" \
  -Pandroid.injected.signing.key.password="<키 비밀번호>"
```

빌드한 APK는 `app/build/outputs/apk/release/app-release.apk`에 생성됩니다. 이미 설치한 앱을 업데이트하려면 처음 설치할 때 쓴 키로 서명해야 합니다.

## Vencord 웹 빌드 갱신

1. AITranslate를 연결한 Vencord 폴더에서 웹 빌드를 만듭니다.

   ```sh
   pnpm buildWeb
   ```

2. 생성된 파일을 릴리스에 올립니다.

   ```sh
   gh release upload vencord-build dist/browser.js dist/browser.css --clobber -R airtaxi/Vendroid
   ```

3. 릴리스 제목의 마지막 단어를 새 값으로 바꿉니다. 앱은 이 값이 설치된 값과 다르면 업데이트가 있다고 판단합니다.

   ```sh
   gh release edit vencord-build --title "AITranslate $(date +%Y%m%d%H%M)" -R airtaxi/Vendroid
   ```
