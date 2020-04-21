import {takeLatest} from 'redux-saga/effects'

// SAGAS
import { authorize } from 'features/authentication/authenticationSaga'
import {submitGuildsQuery} from 'features/guildsList/guildsListSaga'
// import {submitTopicShow} from 'features/topic/topicSaga'

// CONSTANTS
import { AUTH_REQUEST } from 'features/authentication/authenticationConstants'
import { LIST_GUILDS } from 'features/guildsList/guildsListConstants'
// import { FIND_TOPIC, TOPIC_FOUND } from 'features/topic/topicConstants';


 export function * watchGuildsList() {
   yield takeLatest(LIST_GUILDS, submitGuildsQuery);
 }

// export function * watchTopicShow() {
//   yield takeLatest(FIND_TOPIC, submitTopicShow);
// }

export function * watchAuthentication() {
  yield takeLatest(AUTH_REQUEST, authorize);
}