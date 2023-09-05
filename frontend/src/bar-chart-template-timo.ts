import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
//import '@vaadin/button'
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';
import type {Options} from 'highcharts';
import {registerTranslateConfig, use} from "lit-translate";
import {Nordpool} from "Frontend/src/nordpool";

registerTranslateConfig({
    loader: lang => fetch(`${lang}.json`).then(res => res.json())
});

@customElement('bar-chart-template-timo')
export class BarChartTemplateTimo extends LitElement {

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
    averageText: string = '';

    @property()
    average?: number;

    @property()
    currentHour: number = 0;

    @property()
    values?: Array<Nordpool>;

    private getChartOptions(): Options {
        return {
            //rangeSelector: {
            //    enabled: true,
            //    inputEnabled: false,
            //    allButtonsEnabled: false,
            //    verticalAlign: 'bottom',
            //    x: 0,
            //    y: 0,
            //    selected: 0,
            //    buttons: [{
            //        type: 'day',
            //        count: 1,
            //        text: get("column-chart.1d"),
            //    }]
            //},
            chart: {
                type: "column",
                //height: '95%',
            },
            legend: {
                enabled: false
            },
            tooltip: {
                shared: true,
            },
            time: {
                useUTC: false,
                timezone: 'Europe/Helsinki'
            },
            xAxis: {
                tickInterval: 0,
                type: "datetime",
                crosshair: true,
                labels: {
                    format: '{value:%k}'
                },
            },
            yAxis: [{
                title: {
                    text: ''
                    //text: get("general.price-type")
                },
                softMax: this.average! + 1,
                plotLines:
                    [{
                        //label: {
                        //    text: this.averageText
                        //},
                        className: "average-yellow",
                        value: this.average
                    }],
            }],
            plotOptions: {
                column: {
                    //pointPadding: 0,
                    groupPadding: 0,
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
                        value: 5,
                        className: "zone-1"
                    }, {
                        value: 10,
                        className: "zone-2"
                    }, {
                        className: "zone-3"
                    }]
                    ,
                    animation: false
                },
            },
            title: {
                //text: 'Liukuri.fi'
            },
            series: [{
                name: this.seriesTitle,
                type: "column",
                data: this.values!.map(item => [item.time, item.time > 1682888400000 ? item.price * 1.24 : 1.1])
            }],
        };
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
            <!--div class="flex justify-center items-center">
                <vaadin-button class="h-s" theme="tertiary" @click=${this.previous}>
                    <span class="material-icons w-l">chevron_left</span>
                </vaadin-button>
                <h2 class="m-s text-l">${this.chartTitle}</h2>
                <vaadin-button class="h-s" theme="tertiary" @click=${this.next}>
                    <span class="material-icons w-l">chevron_right</span>
                </vaadin-button>
            </div-->
            <vaadin-chart
                    theme="column-timo"
                    style="height: var(--fullscreen-height-timo-column); min-height: 300px"
                    .additionalOptions=${this.getChartOptions()}
            >
            </vaadin-chart>
        `;
    }

}

interface ChartTemplateServerInterface {
    previous(): void;

    next(): void;
}

