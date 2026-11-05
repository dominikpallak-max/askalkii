Askalki - mod Fabric na Minecraft 1.21.11

BUDOWANIE:
1. Wygeneruj szablon na https://fabricmc.net/develop (Minecraft 1.21.11, Fabric API, Java 21).
2. Skopiuj tę zawartość do wygenerowanego projektu (nadpisz src/).
3. Uruchom: ./gradlew build  (Windows: gradlew.bat build)
4. Plik .jar znajdziesz w build/libs/. Wrzuć go do mods/ razem z Fabric API.

UŻYCIE:
W grze wpisz /askalki, żeby otworzyć panel.
- Wszystko - główny włącznik całego moda
- Szybkie podnoszenie - bieganie do itemów
- Opóźnienie - po ilu sekundach od wyrzucenia zacząć biec (0-10 s)
- Złap od razu po wylądowaniu - leci po item w chwili, gdy dotknie ziemi
- Biegnij do itemu (sprint) - sprint podczas biegu

UWAGA: moment podniesienia liczy serwer. Na serwerach z anty-cheatem
auto-bieganie może być traktowane jako cheat. Testuj w singleplayerze.
