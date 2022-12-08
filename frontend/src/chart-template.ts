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
  postfix?: string = 'c/kWh';

  @property()
  values?: Array<number>;

  render() {
    return html`
      <vaadin-chart
          style="height:100%"
          title="${this.chartTitle}"
          subtitle="${this.subtitle}"
          tooltip
          type="column"
          categories='["00","01","02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"]'
          additional-options='{
              //"legend": {
              //    layout": "vertical",
              //    "align": "right",
              //    "verticalAlign": "middle"
              //},
              "xAxis": {
                  "crosshair": true
              }
              //"yAxis": {
              //  "min": -10
              //}
            }'


      >
        <vaadin-chart-series
            title="${this.seriesTitle}"
            unit="${this.unit}"
            .values="${this.values}"
            additional-options='{
                "tooltip": {
                    "pointFormat": "{point.y} ${this.postfix}",
                    "valueDecimals": 2
                }
            }'
        ></vaadin-chart-series>
      </vaadin-chart>
    `;
  }

}
