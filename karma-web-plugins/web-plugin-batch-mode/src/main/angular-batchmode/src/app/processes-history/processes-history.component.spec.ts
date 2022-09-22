import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProcessesHistoryComponent } from './processes-history.component';

describe('HistoryComponent', () => {
  let component: ProcessesHistoryComponent;
  let fixture: ComponentFixture<ProcessesHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProcessesHistoryComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProcessesHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
