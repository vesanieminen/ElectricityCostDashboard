import {html, LitElement, PropertyValues} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';
import type {Options} from 'highcharts';
import {registerTranslateConfig, use} from "lit-translate";
import {Nordpool} from "Frontend/src/nordpool";
import {Plotline} from "Frontend/src/plotline";

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

    @property()
    plotLines?: Array<Plotline>;

    @property({type: Boolean})
    mobileMode = false;

    @property({type: Number})
    min = undefined;

    @property({type: Number})
    max = undefined;

    private getChartOptions(): Options {
        return {
            chart: {
                type: "column",
                animation: false,
                //width: 672
                //width: 385
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
                tickInterval: 3600 * 1000,
                type: "datetime",
                crosshair: true,
                startOnTick: true,
                labels: {
                    format: '{value:%k}'
                },
                //minPadding: 0,
                min: this.min,
                max: this.max,
            },
            yAxis: [{
                title: {
                    text: ''
                    //text: get("general.price-type")
                },
                softMax: this.average == -100 ? undefined : this.average,
                plotLines:
                    this.plotLines!
            }],
            plotOptions: {
                column: this.mobileMode ?
                    {pointPadding: 0, groupPadding: 0, borderRadius: 0} :  // mobile mode
                    {borderRadius: 0}, // desktop mode
                //{groupPadding: 0, borderRadius: 5}, // desktop mode original
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
                data: this.values!.map(item => [item.time, item.time > 1725138000000 ? item.price * 1.255 : item.price * 1.24])
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

