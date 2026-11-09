import { describe, expect, it } from 'vitest';
import { dateKey, filterMovies, groupSeats, selectionTotal, secondsRemaining, upcomingShows } from '../model.js';

const movies = [{ id: 1, title: 'Orbit', author: 'Alex', language: 'EN' }, { id: 2, title: 'Kyoto', author: 'Yuki', language: 'JA' }];
const shows = [{ id: 1, movie: movies[0], start_time: '2030-05-01T18:30:00' }];
describe('catalog filters', () => {
  it('combines search, date, and language against real show data', () => {
    expect(filterMovies(movies, shows, { query: 'alex', date: '2030-05-01', language: 'EN' })).toEqual([movies[0]]);
    expect(filterMovies(movies, shows, { language: 'JA' })).toEqual([]);
  });
  it('separates coming soon movies and saved movies', () => {
    expect(filterMovies(movies, shows, { tab: 'upcoming' })).toEqual([movies[1]]);
    expect(filterMovies(movies, shows, { tab: 'saved', saved: [2] })).toEqual([movies[1]]);
  });
  it('excludes past showtimes', () => expect(upcomingShows(shows, new Date('2031-01-01').getTime())).toEqual([]));
  it('preserves the local calendar date', () => expect(dateKey('2030-05-01T00:30:00')).toBe('2030-05-01'));
});
describe('seat selection', () => {
  const seats = [{ id: 1, price: '100000.00', seat: { row_label: 'B', seat_number: 10 } }, { id: 2, price: '150000.00', seat: { row_label: 'A', seat_number: 2 } }];
  it('adds prices as numbers and includes only selected seats', () => expect(selectionTotal(seats, [1, 2])).toBe(250000));
  it('sorts rows and seat numbers without mutating API data', () => { expect(groupSeats(seats)[0][0]).toBe('A'); expect(seats[0].id).toBe(1); });
  it('never displays a negative reservation countdown', () => expect(secondsRemaining('2030-01-01', new Date('2031-01-01').getTime())).toBe(0));
});
