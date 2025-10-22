package com.arielcardales.arielcardales.Util;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Utilidad para transiciones suaves en la UI
 */
public class Transiciones {

    /**
     * Aplica un efecto de fade in a un nodo
     * @param nodo El nodo a animar
     * @param duracionMs Duración en milisegundos
     */
    public static void fadeIn(Node nodo, double duracionMs) {
        nodo.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), nodo);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Aplica un efecto de fade in con duración por defecto (300ms)
     * @param nodo El nodo a animar
     */
    public static void fadeIn(Node nodo) {
        fadeIn(nodo, 300);
    }

    /**
     * Aplica un efecto de fade out a un nodo
     * @param nodo El nodo a animar
     * @param duracionMs Duración en milisegundos
     * @param onFinished Callback ejecutado al finalizar
     */
    public static void fadeOut(Node nodo, double duracionMs, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), nodo);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        fade.play();
    }

    /**
     * Aplica un efecto de fade out con duración por defecto (200ms)
     * @param nodo El nodo a animar
     * @param onFinished Callback ejecutado al finalizar
     */
    public static void fadeOut(Node nodo, Runnable onFinished) {
        fadeOut(nodo, 200, onFinished);
    }

    /**
     * Transición de cambio de vista: fade out -> cambio -> fade in
     * @param contenedor El contenedor que cambiará de contenido
     * @param nuevoContenido El nuevo nodo a mostrar
     * @param duracionMs Duración total de la transición
     */
    public static void cambiarVistaConFade(javafx.scene.layout.Pane contenedor, Node nuevoContenido, double duracionMs) {
        if (contenedor.getChildren().isEmpty()) {
            // Si está vacío, solo hacer fade in
            contenedor.getChildren().setAll(nuevoContenido);
            fadeIn(nuevoContenido, duracionMs / 2);
        } else {
            // Fade out del contenido actual
            Node actual = contenedor.getChildren().get(0);
            fadeOut(actual, duracionMs / 2, () -> {
                // Cambiar contenido
                contenedor.getChildren().setAll(nuevoContenido);
                // Fade in del nuevo contenido
                fadeIn(nuevoContenido, duracionMs / 2);
            });
        }
    }

    /**
     * Transición de cambio de vista con duración por defecto (400ms)
     * @param contenedor El contenedor que cambiará de contenido
     * @param nuevoContenido El nuevo nodo a mostrar
     */
    public static void cambiarVistaConFade(javafx.scene.layout.Pane contenedor, Node nuevoContenido) {
        cambiarVistaConFade(contenedor, nuevoContenido, 400);
    }
}
