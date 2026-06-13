# Music Wallpaper — Malyk Brown 2026

Live wallpaper Android que muda com a música (crossfade suave entre capas).

## Compilar (GitHub Actions)
1. Faz push deste projecto para o repo (estrutura intacta!)
2. Vai a Actions → Build APK → descarrega o artifact `MusicWallpaper-debug`

## Push via Codespaces (o padrão que funciona)
```bash
unzip -o "MusicWallpaper.zip"
cp -r MusicWallpaper/* .
cp -r MusicWallpaper/.github . 2>/dev/null
rm -rf MusicWallpaper "MusicWallpaper.zip"
git add -A
git commit -m "Music Wallpaper v1 — live wallpaper com crossfade"
git push
```

## Configurar no telefone
1. Instala o APK
2. Abre a app → "Activar acesso às notificações" → liga o Music Wallpaper Listener
3. "Definir como papel de parede" → Aplicar
4. Toca uma música no Spotify/YT Music — o wallpaper muda com crossfade
