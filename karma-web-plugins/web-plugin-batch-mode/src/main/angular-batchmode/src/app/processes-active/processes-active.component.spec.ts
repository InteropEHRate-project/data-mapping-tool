import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProcessesActiveComponent } from './processes-active.component';

describe('ProcessesActiveComponent', () => {
  let component: ProcessesActiveComponent;
  let fixture: ComponentFixture<ProcessesActiveComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProcessesActiveComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProcessesActiveComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
