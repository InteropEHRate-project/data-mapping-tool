import {BrowserModule} from '@angular/platform-browser';
import {APP_INITIALIZER, NgModule} from '@angular/core';
import {AppComponent} from './app.component';
import {HomeComponent} from './home/home.component';
import {ResultsComponent} from './results/results.component';
import {HttpClientJsonpModule, HttpClientModule} from "@angular/common/http";
import {SearchService} from "./common/search.service";
import {AppConfigService} from "./app-config.service";
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {DatasetsComponent} from './results/datasets/datasets.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {SearchComponent} from './search/search.component';
import {FileSizePipe} from "./common/file-size.pipe";

/**
 * load the config file before bootstrapping other components.
 * @param appConfig
 */
const appInitializerFn = (appConfig: AppConfigService) => {
  return () => {
    return appConfig.loadAppConfig();
  }
};

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    ResultsComponent,
    DatasetsComponent,
    SearchComponent,
    FileSizePipe
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    HttpClientJsonpModule,
    NgbModule,
    ReactiveFormsModule,
    FormsModule
  ],
  providers: [
    SearchService,
    AppConfigService,
    {
      provide: APP_INITIALIZER,
      useFactory: appInitializerFn,
      multi: true,
      deps: [AppConfigService]
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
