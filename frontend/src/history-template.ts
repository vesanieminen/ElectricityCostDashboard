import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/vaadin-button'
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';
import type {Options} from 'highcharts';
import {registerTranslateConfig, use} from "lit-translate";

registerTranslateConfig({
    loader: lang => fetch(`${lang}.json`).then(res => res.json())
});

@customElement('history-template')
export class HistoryTemplate extends LitElement {

    @property()
    language: string = 'en';

    // Defer the first update of the component until the strings has been loaded to avoid empty strings being shown
    @state() hasLoadedStrings = false;

    protected shouldUpdate(props: PropertyValues) {
        return this.hasLoadedStrings && super.shouldUpdate(props);
    }

    // Load the initial language and mark that the strings has been loaded so the component can render.
    async connectedCallback() {
        super.connectedCallback();
        await use(this.language);
        this.hasLoadedStrings = true;
    }

    @property()
    chartTitle?: string = '';

    @property()
    seriesTitle?: string = '';

    @property()
    postfix: string = 'c/kWh';

    @property()
    currentHour: number = 0;

    @property()
    values?: Array<Nordpool>;

    private getChartOptions(): Options {
        return {
            rangeSelector: {
                enabled: true,
                verticalAlign: 'top',
                x: 0,
                y: 0,
                selected: 4
                /*buttons: [{
                    type: 'month',
                    // @ts-ignore
                    count: this.values?.at(this.values?.length - 1).time - this.currentHour * 1000,
                    text: get("column-chart.now"),
                    //offsetMin: -86400000,
                    //offsetMax: -86400000,
                }, {
                    type: 'day',
                    count: 1,
                    text: get("column-chart.1d"),

                }, {
                    type: 'day',
                    count: 2,
                    text: get("column-chart.2d"),
                }, {
                    type: 'day',
                    count: 3,
                    text: get("column-chart.3d"),
                }, {
                    type: 'day',
                    count: 5,
                    text: get("column-chart.5d"),
                }, {
                    type: 'all',
                    text: get("column-chart.7d"),
                }]*/
            },
            tooltip: {
                shared: true,
            },
            time: {
                useUTC: false,
                timezone: 'Europe/Helsinki'
            },
            xAxis: {
                type: "datetime",
                crosshair: true,
                plotLines:
                    [{
                        value: this.currentHour * 1000,
                        className: "time"
                    }],
            },
            yAxis: [{
                title: {
                    text: ''
                    //text: get("general.price-type")
                }
            }],
            plotOptions: {
                column: {
                    borderRadius: 5,
                    pointPadding: 0,
                    groupPadding: 0,
                    showInNavigator: true
                },
                series: {
                    tooltip: {
                        valueSuffix: " " + this.postfix,
                        valueDecimals: 2
                    },
                    animation: false
                },
            },
            series: [{
                name: this.seriesTitle,
                type: "column",
                data: this.values!.map(item => [item.time, item.price]),
                showInNavigator: true
            }],
        };
    }

    createRenderRoot() {
        // Do not use a shadow root
        return this;
    }

    render() {
        return html`
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


