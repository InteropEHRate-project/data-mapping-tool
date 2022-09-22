import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {ResultsComponent} from './results.component';
import {Component, Directive, EventEmitter, Input, Output} from "@angular/core";
import {Request} from "../common/request/request.model";
import {Filter} from "../common/filter.model";
import {DatasetsSearchRequest} from "../common/request/datasetsSearchRequest.model";

describe('ResultsComponent', () => {
  let component: ResultsComponent;
  let fixture: ComponentFixture<ResultsComponent>;

  @Component({selector: 'app-search', template: ''})
  class SearchStubComponent {
  }

  @Directive({selector: 'app-datasets'})
  class DatasetsStubComponent {
    @Input() request: Request;
    @Output() allFilters = new EventEmitter<{ [key: string]: Filter[] }>();
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ResultsComponent,
        SearchStubComponent,
        DatasetsStubComponent
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ResultsComponent);
    fixture.autoDetectChanges();
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * make request to load all datasets
   */
  it('#loadAllDatasets()', () => {
    expect(component.request).toBe(undefined);
    component.loadAllDatasets();
    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(null);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(null);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);
  });

  it('#onFilterSelected(): datasets with one filter', () => {
    const selectedFilters: Filter[] = [];
    selectedFilters.push(new Filter({
      type: "tags",
      name: "tolstoy",
      display_name: "tolstoy",
      count: 1
    }));
    expect(component.request).toBe(undefined);
    component.onFilterSelected(selectedFilters[0]);

    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(selectedFilters);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(null);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);

  });

  it('#onFilterSelected(): load datasets with multiple filters', () => {
    const selectedFilters: Filter[] = [];
    selectedFilters.push(new Filter({
      type: "tags",
      name: "tolstoy",
      display_name: "tolstoy",
      count: 1
    }));
    selectedFilters.push(new Filter({
      type: "tags",
      name: "russian",
      display_name: "russian",
      count: 2
    }));
    selectedFilters.push(new Filter({
      type: "organization",
      name: "italy",
      display_name: "italy",
      count: 2
    }));
    expect(component.request).toBe(undefined);
    selectedFilters.forEach(filter => {
      component.onFilterSelected(filter); //sent the request with selected filters
      component.onResultLoaded({}); // if loaded successfully update the selected filter list.
    });

    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(selectedFilters);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(null);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);
  });

  it('#onFilterRemove(): load datasets with filters and then remove them', () => {
    let selectedFilters: Filter[] = [];
    selectedFilters.push(new Filter({
      type: "tags",
      name: "tolstoy",
      display_name: "tolstoy",
      count: 1
    }));
    selectedFilters.push(new Filter({
      type: "tags",
      name: "russian",
      display_name: "russian",
      count: 2
    }));
    selectedFilters.push(new Filter({
      type: "organization",
      name: "italy",
      display_name: "italy",
      count: 2
    }));
    expect(component.request).toBe(undefined);
    selectedFilters.forEach(filter => {
      component.onFilterSelected(filter); //sent the request with selected filters
      component.onResultLoaded({}); // if loaded successfully update the selected filter list.
    });
    component.onFilterRemove(selectedFilters[0]);
    component.onResultLoaded({});
    selectedFilters = selectedFilters.filter(item => item !== selectedFilters[0]);

    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(selectedFilters);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(null);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);
  });

  it('#onSearch(): load datasets with free text search', () => {
    const freeText = 'free text';
    expect(component.request).toBe(undefined);
    component.onSearch(freeText);

    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(null);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(freeText);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);
  });

  it('#onFreeTextRemove(): remove free test search', () => {
    const freeText = 'free text';
    expect(component.request).toBe(undefined);
    component.onSearch(freeText);

    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(null);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(freeText);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);

    component.onFreeTextRemove();
    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(null);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(null);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);
  });

  it('free text + filters', () => {
    const freeText = 'free text';
    const selectedFilters: Filter[] = [];
    selectedFilters.push(new Filter({
      type: "tags",
      name: "tolstoy",
      display_name: "tolstoy",
      count: 1
    }));
    expect(component.request).toBe(undefined);
    component.onSearch(freeText);
    component.onResultLoaded({});
    component.onFilterSelected(selectedFilters[0]);
    component.onResultLoaded({});

    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(selectedFilters);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(freeText);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);
  });

  it('free text + remove filters', () => {
    const freeText = 'free text';
    let selectedFilters: Filter[] = [];
    selectedFilters.push(new Filter({
      type: "tags",
      name: "tolstoy",
      display_name: "tolstoy",
      count: 1
    }));
    selectedFilters.push(new Filter({
      type: "organization",
      name: "italy",
      display_name: "italy",
      count: 2
    }));
    expect(component.request).toBe(undefined);
    component.onSearch(freeText);
    component.onResultLoaded({});
    selectedFilters.forEach(filter => {
      component.onFilterSelected(filter); //sent the request with selected filters
      component.onResultLoaded({}); // if loaded successfully update the selected filter list.
    });

    component.onFilterRemove(selectedFilters[0]);
    component.onResultLoaded({});
    selectedFilters = selectedFilters.filter(item => item !== selectedFilters[0]);

    expect(component.request instanceof DatasetsSearchRequest).toBeTruthy();
    expect((component.request as DatasetsSearchRequest).selectedFilters).toEqual(selectedFilters);
    expect((component.request as DatasetsSearchRequest).freeTextSearch).toEqual(freeText);
    expect((component.request as DatasetsSearchRequest).row).toEqual(10);
    expect((component.request as DatasetsSearchRequest).start).toEqual(null);
  });

});
