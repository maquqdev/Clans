# Klany



## Funkcje

- **Zarządzanie Klanami**: Tworzenie, dołączanie i zarządzanie klanami z konfigurowalnymi rolami (Lider, Zastępca, Członek).
- **System Punktów**: Dostawanie punktów za zabójstwa graczy.
- **Wiele typów baz danych**: Obsługa wielu baz danych (MongoDB, MySQL, FLAT).
- **Modularny Projekt**: Czyste API umożliwiające programistom rozszerzanie funkcjonalności.

## Instalacja

1. Pobierz najnowszą wersje z [releases page](https://github.com/maquqdev/Clans/releases).
2. Umieść plik JAR w katalogu `plugins` swojego serwera.
3. Zrestartuj serwer.
4. Skonfiguruj plugin w folderze `plugins/Clans`.

## Uprawnienia

- `clans.create` – Tworzenie klanów
- `clans.invite` – Zapraszanie graczy do klanu
- `clans.kick` – Wyrzucanie członków z klanu
- `clans.upgrade` – Ulepszanie właściwości klanu
- `clans.admin` – Komendy administracyjne

## Komendy

- `/klan stworz <tag>` – Utwórz nowy klan
- `/klan zapros <gracz>` – Zaproś gracza do swojego klanu
- `/klan dolacz` – Dołącz do klanu (jeśli zostałeś zaproszony)
- `/klan opusc` – Opuść obecny klan
- `/klan info [tag]` – Wyświetl informacje o klanie
- `/klan zastepca [gracz]` - Dodawanie zastępcy osobie z klanu
- `/klan menu` – Otwiera menu ulepszeń, zarządzania członkami
- `/klan usun` – Usuwanie klanu

## Konfiguracja
Przejrzysta, prosta w JSON - zobacz sobie jak konfigurować JSON [tutaj](https://learnxinyminutes.com/docs/json/)

## API dla Deweloperów

Łatwe, fajne przejrzyste - zobacz sobie moduł API.

## Budowanie projektu

1. Sklonuj repozytorium
2. Uruchom `./gradlew build`
3. Znajdź zbudowany plik JAR w `spigot/build/libs/`

## Wkład w Projekt

Wkład w rozwój jest mile widziany! Możesz zgłaszać pull requesty lub otwierać zgłoszenia błędów i propozycje nowych funkcji.
Również przyjmuje krytykę - jeżeli coś jest źle zrobione, napisz do mnie!

## Licencja

Projekt jest licencjonowany na zasadach licencji MIT – zobacz plik LICENSE, aby uzyskać więcej informacji.


