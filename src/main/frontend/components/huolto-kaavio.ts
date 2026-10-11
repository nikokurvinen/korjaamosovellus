/*
 * <huolto-kaavio> – Chart.js-kirjaston Web Component -kääre (vaatimus 33).
 *
 * Java-puoli (HuoltoKaavio.java) asettaa kaavion määrityksen JSON-merkkijonona `config`-propertyyn.
 * Lit kutsuu updated()-metodia aina propertyn muuttuessa, jolloin olemassa oleva kaavio päivitetään
 * paikallaan (chart.update) – uuden kaavion luominen joka kerta aiheuttaisi välkkymistä ja
 * vuotaisi canvas-kontekstien muistia.
 *
 * Klikkaus kaavion palkkiin/sektoriin välitetään palvelimelle `segmentti-klikattu`-eventtinä,
 * jolloin Java voi esim. navigoida hakunäkymään valitulla tilalla.
 */
import { LitElement, html, css, PropertyValues } from 'lit';
import Chart from 'chart.js/auto';

export class HuoltoKaavio extends LitElement {
  static properties = {
    config: { type: String },
  };

  static styles = css`
    :host {
      display: block;
      position: relative;
      min-height: 220px;
    }
    .kehys {
      position: relative;
      width: 100%;
      height: 100%;
    }
  `;

  declare config: string;
  private chart?: Chart;

  constructor() {
    super();
    this.config = '';
  }

  render() {
    return html`<div class="kehys"><canvas></canvas></div>`;
  }

  protected updated(changed: PropertyValues) {
    if (!changed.has('config') || !this.config) {
      return;
    }
    const maaritys = JSON.parse(this.config);
    // Teeman fontti myös kaavioon, jotta se ei poikkea muusta sovelluksesta.
    Chart.defaults.font.family = getComputedStyle(this).getPropertyValue('--lumo-font-family') || 'sans-serif';

    if (this.chart && (this.chart.config as any).type === maaritys.type) {
      this.chart.data = maaritys.data;
      this.chart.options = this.lisaaKlikkaus(maaritys.options ?? {});
      this.chart.update();
      return;
    }
    this.chart?.destroy();
    const canvas = this.renderRoot.querySelector('canvas') as HTMLCanvasElement;
    this.chart = new Chart(canvas, {
      type: maaritys.type,
      data: maaritys.data,
      options: this.lisaaKlikkaus(maaritys.options ?? {}),
    });
  }

  private lisaaKlikkaus(options: any) {
    return {
      responsive: true,
      maintainAspectRatio: false,
      ...options,
      onClick: (_e: unknown, elements: { index: number }[]) => {
        if (!elements.length || !this.chart) return;
        const label = this.chart.data.labels?.[elements[0].index];
        this.dispatchEvent(new CustomEvent('segmentti-klikattu', { detail: { label: String(label) } }));
      },
    };
  }

  disconnectedCallback() {
    // Vaadin irrottaa elementin DOMista navigoitaessa; kaavio tuhotaan, jotta canvas vapautuu.
    this.chart?.destroy();
    this.chart = undefined;
    super.disconnectedCallback();
  }

  connectedCallback() {
    super.connectedCallback();
    // Uudelleenliitettäessä (esim. paluu näkymään) kaavio piirretään uudelleen viimeisimmällä datalla.
    if (this.config && this.hasUpdated) {
      this.requestUpdate('config', '');
    }
  }
}

customElements.define('huolto-kaavio', HuoltoKaavio);
