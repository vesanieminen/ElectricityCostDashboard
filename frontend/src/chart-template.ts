import {css, html, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';

@customElement('chart-template')
export class ChartTemplate extends LitElement {

  static styles = css`
  `;

  @property()
  chartTitle?: string = '';

  @property()
  subtitle?: string = '';

  @property()
  seriesTitle?: string = '';

  @property()
  unit?: string = '';

  @property()
  values?: Array<number>;

  render() {
    return html`
      <vaadin-chart
          title="${this.chartTitle}"
          subtitle="${this.subtitle}"
          type="column"
          additional-options='{
              "legend": {
                  "layout": "vertical",
                  "align": "right",
                  "verticalAlign": "middle"
              },
              "xAxis": {
                  "crosshair": true
              }
            }'


      >
        <vaadin-chart-series title="${this.seriesTitle}" unit="${this.unit}"
                             .values="${this.values}"></vaadin-chart-series>
      </vaadin-chart>
    `;
  }

}
