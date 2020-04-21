import { LIST_GUILDS, GUILDS_FOUND } from './guildsListConstants';

export const findGuilds = () => ({
  type: LIST_GUILDS
});

export const loadGuilds = results => ({
  type: GUILDS_FOUND,
  results
});
