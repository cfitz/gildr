import { AUTH_REQUEST} from './authenticationConstants'

export const authorize = (name, password) => ({
    type: AUTH_REQUEST,
    payload: { name, password }
  });