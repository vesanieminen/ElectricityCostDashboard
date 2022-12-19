import {css, html, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import '@vaadin/vaadin-button'
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';
import type {Options} from 'highcharts';

@customElement('chart-template')
export class ChartTemplate extends LitElement {

    static styles = css`
  `;

    @property()
    chartTitle?: string = '';

    @property()
    seriesTitle?: string = '';

    @property()
    postfix: string = 'c/kWh';

    @property()
    averageText: string = '';

    @property()
    average?: number;

    @property()
    currentHour: number = 0;

    @property()
    values?: Array<Nordpool>;

    private getChartOptions(): Options {
        return {
            chart: {
                type: "column"
            },
            tooltip: {
                shared: true,
            },
            xAxis: {
                type: "datetime",
                crosshair: true,
                plotLines:
                    [{
                        value: this.currentHour,
                        className: "time"
                    }],
            },
            yAxis: [{
                title: {
                    text: ""
                },
                plotLines:
                    [{
                        //label: {
                        //    text: this.averageText
                        //},
                        value: this.average
                    }, {
                        className: "average-per-2",
                        value: this.average! / 2
                    }],
            }],
            plotOptions: {
                column: {
                    borderRadius: 5
                },
                series: {
                    tooltip: {
                        valueSuffix: " " + this.postfix,
                        valueDecimals: 2
                    },
                    zones: [{
                        value: -10,
                        className: "zone-0"
                    }, {
                        value: this.average! / 2,
                        className: "zone-1"
                    }, {
                        value: this.average,
                        className: "zone-2"
                    }, {
                        className: "zone-3"
                    }]
                    ,
                    animation: false
                },
            },
            series: [{
                name: this.seriesTitle,
                type: "column",
                //data: this.values
                data: [...this.values!.map(item => [item.time, item.price * 1.1])]
            }],
        };
    }

    connectedCallback() {
        super.connectedCallback();
        //this.classList.add('h-full');
    }

    createRenderRoot() {
        // Do not use a shadow root
        return this;
    }

    previous() {
        this.$server!.previous();
    }

    next() {
        this.$server!.next();
    }

    private $server?: ChartTemplateServerInterface;

    render() {
        return html`
            <div class="flex justify-center items-center">
                <vaadin-button class="h-s" theme="tertiary" @click=${this.previous}>
                    <span class="material-icons w-l">chevron_left</span>
                </vaadin-button>
                <h2 class="m-s text-l">${this.chartTitle}</h2>
                <vaadin-button class="h-s" theme="tertiary" @click=${this.next}>
                    <span class="material-icons w-l">chevron_right</span>
                </vaadin-button>
            </div>
            <vaadin-chart
                    theme="column"
                    style="height: 100%"
                    .additionalOptions=${this.getChartOptions()}
            >
            </vaadin-chart>
        `;
    }

}

interface Nordpool {
    time: number;
    price: number;
}

interface ChartTemplateServerInterface {
    previous(): void;

    next(): void;
}

