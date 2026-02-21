import pytest
from src.session.Session import Session, SessionState
from datetime import datetime, timedelta


def test_session_state_transitions():
    s = Session(id='1', transportType='RDP')
    assert s.state == SessionState.PENDING
    created = s.createdAt
    s.markConnecting()
    assert s.state == SessionState.CONNECTING
    assert s.updatedAt >= created
    s.markActive()
    assert s.state == SessionState.ACTIVE
    s.markTerminated()
    assert s.state == SessionState.TERMINATED


def test_failed_path():
    s = Session(id='2', transportType='RUSTDESK')
    s.markConnecting()
    s.markFailed('no route')
    assert s.state == SessionState.FAILED
    assert s.failureReason == 'no route'
