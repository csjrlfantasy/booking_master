import {Share} from 'react-native';
import {AppConfig} from '../constants';

function resolveShareOrigin() {
  return (AppConfig as any).share?.publicOrigin || 'http://127.0.0.1:3000';
}

export function buildTicketShareUrl(ticketId: number | string) {
  return `${resolveShareOrigin()}/shop/tickets/${ticketId}`;
}

export async function shareTicket(title: string, ticketId: number | string) {
  const url = buildTicketShareUrl(ticketId);
  return Share.share({
    title,
    message: `${title} ${url}`,
    url,
  });
}
