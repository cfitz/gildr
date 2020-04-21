import { listGuilds } from 'clients/GildrClient';
import { LIST_GUILDS, GUILDS_FOUND } from './guildsListConstants';

import {put, call} from 'redux-saga/effects'

export  function *  submitGuildsQuery() {
  try {
    const payload = yield call(listGuilds);
    yield put({type: GUILDS_FOUND, payload});
  } catch (err) {
    yield put({type: 'GUILDS_ERROR', err })
  }
}
