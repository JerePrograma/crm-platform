package com.gestudio.crm.business;

import java.util.List;
import java.util.Set;

/** Public business references confirmed on 2026-10-14; no customer records or credentials. */
public final class ScalarisCatalog {

  public static final String NAME = "Scalaris";
  public static final String SLUG = "scalaris-local";
  public static final String TAGLINE = "Tu tecnología, en buenas manos";
  public static final String DESCRIPTOR = "Servicio técnico · Desarrollo · Venta a pedido";
  public static final String WEBSITE = "https://scalaris.pages.dev/";
  public static final String WHATSAPP_DISPLAY = "11 7278-9289";
  public static final String WHATSAPP_URL = "https://wa.me/5491172789289";
  public static final String COVERAGE = "Zona Oeste, CABA y Mar del Plata";
  public static final String BRAND_COLOR = "#145CDB";
  public static final List<String> SERVICE_LINES =
      List.of("Servicio técnico", "Desarrollo", "Venta a pedido");
  public static final List<String> OPERATING_GUIDANCE =
      List.of(
          "Retiro y entrega de equipos a coordinar según servicio, localidad y caso.",
          "Atención remota para desarrollo y soporte cuando el caso lo permite.",
          "Una reparación física requiere recibir el equipo.",
          "Confirmar diagnóstico, alcance, presupuesto, disponibilidad y plazos en cada consulta.",
          "Las exclusiones y solicitudes de no contacto tienen prioridad.",
          "Revisar y enviar cada comunicación manualmente; el CRM local no envía mensajes reales.");
  public static final Set<String> MARKETING_PERMISSIONS =
      Set.of(
          "PROSPECT_READ",
          "PROSPECT_WRITE",
          "PROSPECT_ASSIGN",
          "IMPORT_PREVIEW",
          "IMPORT_EXECUTE",
          "DUPLICATE_RESOLVE",
          "ACTIVITY_WRITE",
          "OPPORTUNITY_WRITE",
          "CAMPAIGN_READ",
          "CAMPAIGN_WRITE",
          "CAMPAIGN_APPROVE",
          "MESSAGE_DRAFT",
          "MESSAGE_SIMULATE",
          "REPORT_READ");
  public static final List<TagSeed> TAGS =
      List.of(
          new TagSeed("Servicio técnico", "#145CDB"),
          new TagSeed("Desarrollo", "#0B1F33"),
          new TagSeed("Venta a pedido", "#425A70"),
          new TagSeed("PC", "#145CDB"),
          new TagSeed("Notebook", "#145CDB"),
          new TagSeed("Soporte remoto", "#145CDB"),
          new TagSeed("Armado de PC", "#145CDB"),
          new TagSeed("Sitio web", "#0B1F33"),
          new TagSeed("Sistema a medida", "#0B1F33"),
          new TagSeed("Automatización", "#0B1F33"),
          new TagSeed("Compra por encargo", "#425A70"),
          new TagSeed("Zona Oeste", "#425A70"),
          new TagSeed("CABA", "#425A70"),
          new TagSeed("Mar del Plata", "#425A70"),
          new TagSeed("Particulares", "#425A70"),
          new TagSeed("Comercios", "#425A70"),
          new TagSeed("Pymes", "#425A70"),
          new TagSeed("Profesionales", "#425A70"));

  private static final String SIGNATURE =
      "\n\nSCALARIS · " + TAGLINE + "\nWhatsApp: " + WHATSAPP_DISPLAY + "\n" + WEBSITE;
  public static final List<TemplateSeed> TEMPLATES =
      List.of(
          new TemplateSeed(
              "Scalaris · Servicio técnico · Presentación",
              "EMAIL",
              "Servicio técnico para PC y notebooks",
              "Hola, soy de Scalaris. Brindamos diagnóstico, mantenimiento, reparación y mejoras de PC y notebooks.\n\n"
                  + "Si necesitás revisar un equipo, contame qué ocurre y en qué localidad estás para coordinar la consulta. "
                  + "El retiro y la entrega se acuerdan según el caso.\n\n"
                  + COVERAGE
                  + "."
                  + SIGNATURE),
          new TemplateSeed(
              "Scalaris · Desarrollo · Presentación",
              "EMAIL",
              "Desarrollo para tu negocio",
              "Hola, soy de Scalaris. Desarrollamos sitios web, sistemas a medida y automatizaciones.\n\n"
                  + "Si tenés una necesidad concreta, podemos conversar sobre cómo trabajan hoy, qué querés resolver "
                  + "y el alcance que tendría una solución. Con esa información preparamos una propuesta."
                  + SIGNATURE),
          new TemplateSeed(
              "Scalaris · Venta a pedido · Consulta",
              "EMAIL",
              "Cotización de tecnología a pedido",
              "Hola, soy de Scalaris. Cotizamos equipos informáticos, componentes, periféricos, almacenamiento "
                  + "y accesorios a pedido.\n\nContame qué producto buscás y para qué lo necesitás. "
                  + "Confirmamos opciones, disponibilidad, condiciones y plazos antes de avanzar."
                  + SIGNATURE),
          new TemplateSeed(
              "Scalaris · Seguimiento de consulta",
              "EMAIL",
              "Seguimiento de tu consulta a Scalaris",
              "Hola, retomo la consulta que recibimos en Scalaris.\n\n"
                  + "Si todavía necesitás avanzar, contame si cambió algo o qué información te falta para decidir. "
                  + "Si preferís cerrar la consulta, avisame y lo dejamos registrado."
                  + SIGNATURE),
          new TemplateSeed(
              "Scalaris · Servicio técnico · Respuesta",
              "WHATSAPP",
              "Consulta de servicio técnico",
              "Hola, soy de Scalaris. Para orientarte con el equipo, contame si es PC o notebook, qué problema presenta "
                  + "y en qué localidad estás. El diagnóstico y el retiro o entrega se coordinan según el caso."),
          new TemplateSeed(
              "Scalaris · Desarrollo · Respuesta",
              "WHATSAPP",
              "Consulta de desarrollo",
              "Hola, soy de Scalaris. Contame qué necesitás para tu negocio: un sitio web, un sistema a medida "
                  + "o una automatización. Podemos revisar cómo trabajan hoy y definir el alcance antes de cotizar."),
          new TemplateSeed(
              "Scalaris · Venta a pedido · Respuesta",
              "WHATSAPP",
              "Consulta de compra por encargo",
              "Hola, soy de Scalaris. Contame qué producto buscás, para qué lo necesitás y si tenés un presupuesto "
                  + "de referencia. Confirmamos opciones, disponibilidad, condiciones y plazos antes de avanzar."));

  private ScalarisCatalog() {}

  public record TagSeed(String name, String color) {}

  public record TemplateSeed(String name, String channel, String subject, String body) {}
}
