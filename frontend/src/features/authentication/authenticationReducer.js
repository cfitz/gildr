import {createReducer} from 'common/utils/reducerUtils';
import { AUTH_FAILURE, AUTH_SUCCESS } from './authenticationConstants'

const initialState = {
    token: localStorage.getItem('token'),
    error: null
  };


export function authSuccess(state = {}, payload) {
    return { ...state, token: payload };
}

export function authFailure(state = {}, payload ) {
    return { ...state, error: payload}
}

export default createReducer(initialState, {
    [AUTH_SUCCESS]: authSuccess,
    [AUTH_FAILURE]: authFailure
})