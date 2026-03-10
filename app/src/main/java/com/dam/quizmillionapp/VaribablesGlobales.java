package com.dam.quizmillionapp;

public class VaribablesGlobales {
    public static boolean musicaActivada = true;
    /*
    Añadir en cada activity en el OnCreate estas lineas,
    que lo que hacen es traerse heredado el estado de la musica
    de la actividad anterior.:

               btnMusica.setImageResource(VaribablesGlobales.musicaActivada ?
               R.drawable.ic_music_on : R.drawable.ic_music_off);

     Opcional, metodo ToogleMusica:
     ---------------------------------------------------------------------

        private void toggleMusica() {
        VaribablesGlobales.musicaActivada = !VaribablesGlobales.musicaActivada;

        Intent intent = new Intent(this, MusicService.class);

        if (VaribablesGlobales.musicaActivada) {
            intent.setAction(MusicService.ACTION_PLAY);
            btnMusica.setImageResource(R.drawable.ic_music_on);
        } else {
            intent.setAction(MusicService.ACTION_PAUSE);
            btnMusica.setImageResource(R.drawable.ic_music_off);
        }

        startService(intent);
    }
    ---------------------------------------------------------------------

     */
}
