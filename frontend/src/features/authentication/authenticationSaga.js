import { call, put } from 'redux-saga/effects';
import { login } from 'clients/GildrClient'
import { AUTH_SUCCESS, AUTH_FAILURE } from './authenticationConstants';

export function * authorize({ payload: { name, password } }) {
  try {
    const { token } = yield call(login, {name, password});
    yield put({ type: AUTH_SUCCESS, payload: token });
    localStorage.setItem('token', token);
  } catch(error) {
    let message;
    switch (error.status) {
      case 500: message = 'Internal Server Error'; break;
      case 401: message = 'Invalid credentials'; break;
      default: message = 'Something went wrong';
    }
    yield put({ type: AUTH_FAILURE, payload: message });
    localStorage.removeItem('token');
  }
}
